package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.dto.ResultadoIntegridade;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.SegurancaUtils;
import com.umc.pediupatrao.security.UsuarioLogado;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Registro e consulta da auditoria.
 *
 * Integridade: cada registro recebe uma sequencia e o hash SHA-256 do seu conteudo somado
 * ao hash do registro anterior. Alterar ou apagar um registro diretamente no banco quebra a
 * cadeia, o que e detectado por {@link #verificarIntegridade()}.
 *
 * Observacao: o metodo de registro e synchronized para garantir a sequencia em uma unica
 * instancia da aplicacao (cenario do projeto). Para varias instancias seria necessario um
 * contador atomico no banco.
 */
@Service
public class AuditoriaService {

    public static final String HASH_INICIAL = "GENESIS";

    public static final String ENTIDADE_PEDIDO = "PEDIDO";
    public static final String ENTIDADE_CLIENTE = "CLIENTE";
    public static final String ENTIDADE_PRODUTO = "PRODUTO";
    public static final String ENTIDADE_USUARIO = "USUARIO";
    public static final String ENTIDADE_SISTEMA = "SISTEMA";

    private final MongoTemplate mongoTemplate;
    private final AuditoriaRepository auditoriaRepository;
    private final Clock clock;
    private final ZoneId zona;

    public AuditoriaService(MongoTemplate mongoTemplate,
                            AuditoriaRepository auditoriaRepository,
                            Clock clock,
                            @Value("${app.zona-horaria:America/Sao_Paulo}") String zonaHoraria) {
        this.mongoTemplate = mongoTemplate;
        this.auditoriaRepository = auditoriaRepository;
        this.clock = clock;
        this.zona = ZoneId.of(zonaHoraria);
    }

    public RegistroAuditoria registrar(TipoOperacao operacao, String entidade, String entidadeId,
                                       List<AlteracaoCampo> alteracoes, String justificativa) {
        return registrar(SegurancaUtils.usuarioAtual(), operacao, entidade, entidadeId, alteracoes, justificativa);
    }

    public synchronized RegistroAuditoria registrar(UsuarioLogado usuario, TipoOperacao operacao,
                                                    String entidade, String entidadeId,
                                                    List<AlteracaoCampo> alteracoes, String justificativa) {
        RegistroAuditoria ultimo = auditoriaRepository.findTopByOrderBySequenciaDesc().orElse(null);

        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setSequencia(ultimo == null ? 1L : ultimo.getSequencia() + 1);
        // O MongoDB guarda datas com precisao de milissegundos; truncar evita divergencia no hash.
        registro.setDataHora(Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
        registro.setUsuario(usuario.username());
        registro.setPerfil(usuario.perfil());
        registro.setOperacao(operacao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setAlteracoes(alteracoes != null ? new ArrayList<>(alteracoes) : new ArrayList<>());
        registro.setJustificativa(justificativa);
        registro.setHashAnterior(ultimo == null ? HASH_INICIAL : ultimo.getHash());
        registro.setHash(calcularHash(registro));

        return mongoTemplate.insert(registro);
    }

    /** Registra tentativa de acesso indevido (sem dados sensiveis da requisicao). */
    public void registrarAcessoNegado(String recurso) {
        registrar(TipoOperacao.ACESSO_NEGADO, ENTIDADE_SISTEMA, null,
                List.of(new AlteracaoCampo("recurso", null, recurso)), null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Page<RegistroAuditoria> consultar(FiltroAuditoria filtro, Pageable pageable) {
        if (filtro.de() != null && filtro.ate() != null && filtro.de().isAfter(filtro.ate())) {
            throw new com.umc.pediupatrao.exception.ValidacaoException(
                    "A data inicial não pode ser posterior à data final.");
        }
        Query query = new Query(montarCriterios(filtro));
        long total = mongoTemplate.count(query, RegistroAuditoria.class);
        query.with(Sort.by(Sort.Direction.DESC, "sequencia")).with(pageable);
        List<RegistroAuditoria> registros = mongoTemplate.find(query, RegistroAuditoria.class);
        return new PageImpl<>(registros, pageable, total);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public RegistroAuditoria buscarPorId(String id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new com.umc.pediupatrao.exception.RecursoNaoEncontradoException(
                        "Registro de auditoria não encontrado."));
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<RegistroAuditoria> historicoDaEntidade(String entidade, String entidadeId) {
        return auditoriaRepository.findByEntidadeAndEntidadeIdOrderBySequenciaAsc(entidade, entidadeId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ResultadoIntegridade verificarIntegridade() {
        List<RegistroAuditoria> registros = auditoriaRepository.findAllByOrderBySequenciaAsc();
        String hashEsperadoAnterior = HASH_INICIAL;
        long sequenciaEsperada = 1;
        for (RegistroAuditoria r : registros) {
            if (r.getSequencia() != sequenciaEsperada) {
                return new ResultadoIntegridade(false, registros.size(), sequenciaEsperada,
                        "Registro ausente ou fora de ordem na sequência " + sequenciaEsperada + ".");
            }
            if (!hashEsperadoAnterior.equals(r.getHashAnterior())) {
                return new ResultadoIntegridade(false, registros.size(), r.getSequencia(),
                        "Encadeamento rompido na sequência " + r.getSequencia() + ".");
            }
            if (!calcularHash(r).equals(r.getHash())) {
                return new ResultadoIntegridade(false, registros.size(), r.getSequencia(),
                        "Conteúdo alterado na sequência " + r.getSequencia() + ".");
            }
            hashEsperadoAnterior = r.getHash();
            sequenciaEsperada++;
        }
        return new ResultadoIntegridade(true, registros.size(), null,
                "Todos os " + registros.size() + " registros estão íntegros.");
    }

    Criteria montarCriterios(FiltroAuditoria filtro) {
        List<Criteria> criterios = new ArrayList<>();
        if (temTexto(filtro.entidade())) {
            criterios.add(Criteria.where("entidade").is(filtro.entidade().trim().toUpperCase()));
        }
        if (temTexto(filtro.entidadeId())) {
            criterios.add(Criteria.where("entidadeId").is(filtro.entidadeId().trim()));
        }
        if (temTexto(filtro.usuario())) {
            criterios.add(Criteria.where("usuario")
                    .regex("^" + Pattern.quote(filtro.usuario().trim()), "i"));
        }
        if (filtro.operacao() != null) {
            criterios.add(Criteria.where("operacao").is(filtro.operacao()));
        }
        LocalDate de = filtro.de();
        LocalDate ate = filtro.ate();
        if (de != null || ate != null) {
            Criteria c = Criteria.where("dataHora");
            if (de != null) {
                c = c.gte(de.atStartOfDay(zona).toInstant());
            }
            if (ate != null) {
                c = c.lt(ate.plusDays(1).atStartOfDay(zona).toInstant());
            }
            criterios.add(c);
        }
        return criterios.isEmpty() ? new Criteria() : new Criteria().andOperator(criterios.toArray(new Criteria[0]));
    }

    static String calcularHash(RegistroAuditoria r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getSequencia()).append('|')
          .append(r.getDataHora() != null ? r.getDataHora().toEpochMilli() : "").append('|')
          .append(nvl(r.getUsuario())).append('|')
          .append(nvl(r.getPerfil())).append('|')
          .append(r.getOperacao() != null ? r.getOperacao().name() : "").append('|')
          .append(nvl(r.getEntidade())).append('|')
          .append(nvl(r.getEntidadeId())).append('|');
        if (r.getAlteracoes() != null) {
            for (AlteracaoCampo a : r.getAlteracoes()) {
                sb.append(nvl(a.getCampo())).append('=')
                  .append(nvl(a.getValorAnterior())).append("->")
                  .append(nvl(a.getValorNovo())).append(';');
            }
        }
        sb.append('|').append(nvl(r.getJustificativa()))
          .append('|').append(nvl(r.getHashAnterior()));
        return sha256(sb.toString());
    }

    private static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}
