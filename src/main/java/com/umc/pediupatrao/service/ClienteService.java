package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ClienteRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.util.Diferencas;
import com.umc.pediupatrao.util.MascaraDados;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository,
                          PedidoRepository pedidoRepository,
                          AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll(Sort.by("nome"));
    }

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente criar(ClienteRequest dados) {
        validar(dados);
        Cliente cliente = new Cliente();
        aplicar(cliente, dados);
        Cliente salvo = clienteRepository.save(cliente);
        auditoriaService.registrar(TipoOperacao.CRIACAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                salvo.getId(), Diferencas.inclusao(retratoParaAuditoria(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente atualizar(String id, ClienteRequest dados) {
        validar(dados);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado para atualização."));

        Map<String, String> antes = retratoParaAuditoria(cliente);
        aplicar(cliente, dados);
        Map<String, String> depois = retratoParaAuditoria(cliente);
        List<AlteracaoCampo> alteracoes = Diferencas.comparar(antes, depois);

        Cliente salvo = clienteRepository.save(cliente);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                    salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    /**
     * Exclusao restrita ao gerente e bloqueada quando o cliente possui pedidos,
     * para nao deixar pedidos orfaos nem apagar historico.
     */
    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        if (pedidoRepository.existsByClienteId(id)) {
            throw new RegraNegocioException("Cliente possui pedidos registrados e não pode ser excluído.");
        }
        Map<String, String> antes = retratoParaAuditoria(cliente);
        clienteRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, AuditoriaService.ENTIDADE_CLIENTE,
                id, Diferencas.comparar(antes, Map.of()), null);
    }

    // ------------------------------------------------------------------ auxiliares

    void validar(ClienteRequest d) {
        List<String> erros = new ArrayList<>();
        if (d == null) {
            throw new ValidacaoException("Dados do cliente ausentes.");
        }
        if (vazio(d.nome())) {
            erros.add("Informe o nome.");
        } else if (d.nome().trim().length() > 120) {
            erros.add("O nome pode ter no máximo 120 caracteres.");
        }
        String digitosTelefone = d.telefone() == null ? "" : d.telefone().replaceAll("\\D", "");
        if (digitosTelefone.length() < 10 || digitosTelefone.length() > 11) {
            erros.add("Informe um telefone válido com DDD (10 ou 11 dígitos).");
        }
        if (!vazio(d.cep()) && !d.cep().trim().matches("\\d{5}-?\\d{3}")) {
            erros.add("CEP inválido (formato 00000-000).");
        }
        if (!vazio(d.estado()) && !d.estado().trim().matches("[A-Za-z]{2}")) {
            erros.add("Estado deve ter 2 letras (ex.: SP).");
        }
        for (String campo : new String[]{d.logradouro(), d.numero(), d.complemento(), d.bairro(), d.cidade()}) {
            if (campo != null && campo.length() > 150) {
                erros.add("Campos de endereço podem ter no máximo 150 caracteres.");
                break;
            }
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    /** Copia campo a campo (sem aceitar id ou outros atributos vindos da requisicao). */
    private void aplicar(Cliente c, ClienteRequest d) {
        c.setNome(limpar(d.nome()));
        c.setTelefone(limpar(d.telefone()));
        c.setCep(limpar(d.cep()));
        c.setLogradouro(limpar(d.logradouro()));
        c.setNumero(limpar(d.numero()));
        c.setComplemento(limpar(d.complemento()));
        c.setBairro(limpar(d.bairro()));
        c.setCidade(limpar(d.cidade()));
        c.setEstado(d.estado() == null || d.estado().isBlank() ? null : d.estado().trim().toUpperCase());
    }

    /** Campos auditados. O telefone e mascarado (minimizacao de dados pessoais). */
    static Map<String, String> retratoParaAuditoria(Cliente c) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("nome", c.getNome());
        m.put("telefone", MascaraDados.telefone(c.getTelefone()));
        m.put("cep", c.getCep());
        m.put("logradouro", c.getLogradouro());
        m.put("numero", c.getNumero());
        m.put("complemento", c.getComplemento());
        m.put("bairro", c.getBairro());
        m.put("cidade", c.getCidade());
        m.put("estado", c.getEstado());
        return m;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpar(String s) {
        return vazio(s) ? null : s.trim();
    }
}
