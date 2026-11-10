package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.security.SegurancaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Administracao de usuarios. Todas as operacoes sao exclusivas do ADMIN e aplicam protecoes
 * contra atribuicao indevida de privilegios:
 *  - o perfil precisa ser um dos tres perfis validos (valor informado e validado no servidor);
 *  - o administrador nao altera o proprio perfil nem exclui a propria conta;
 *  - o sistema nunca fica sem nenhum ADMIN.
 */
@Service
public class UsuarioService {

    static final int SENHA_MINIMA = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll(Sort.by("username"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario criar(UsuarioRequest dados) {
        List<String> erros = new ArrayList<>();
        String username = normalizarUsername(dados == null ? null : dados.username(), erros);
        Perfil perfil = Perfil.fromEstrito(dados == null ? null : dados.perfil());
        if (perfil == null) {
            erros.add("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
        if (dados == null || dados.password() == null || dados.password().length() < SENHA_MINIMA) {
            erros.add("A senha deve ter no mínimo " + SENHA_MINIMA + " caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new RegraNegocioException("Já existe um usuário com esse nome.");
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(dados.password()));
        u.setRole(perfil.name());
        Usuario salvo = usuarioRepository.save(u);

        auditoriaService.registrar(TipoOperacao.CRIACAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO, salvo.getId(),
                List.of(new AlteracaoCampo("username", null, username),
                        new AlteracaoCampo("perfil", null, perfil.name()),
                        new AlteracaoCampo("senha", null, "[DEFINIDA]")), null);
        return salvo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizar(String id, UsuarioRequest dados) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        List<String> erros = new ArrayList<>();
        String username = normalizarUsername(dados == null ? null : dados.username(), erros);
        Perfil novoPerfil = Perfil.fromEstrito(dados == null ? null : dados.perfil());
        if (novoPerfil == null) {
            erros.add("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
        boolean trocaSenha = dados != null && dados.password() != null && !dados.password().isBlank();
        if (trocaSenha && dados.password().length() < SENHA_MINIMA) {
            erros.add("A senha deve ter no mínimo " + SENHA_MINIMA + " caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        Perfil perfilAtual = existente.getPerfil();
        boolean ehOProprioUsuario = existente.getUsername().equals(SegurancaUtils.usuarioAtual().username());
        if (ehOProprioUsuario && novoPerfil != perfilAtual) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (perfilAtual == Perfil.ADMIN && novoPerfil != Perfil.ADMIN && contarAdmins() <= 1) {
            throw new RegraNegocioException("O sistema precisa manter ao menos um administrador.");
        }
        if (!username.equals(existente.getUsername()) && usuarioRepository.existsByUsername(username)) {
            throw new RegraNegocioException("Já existe um usuário com esse nome.");
        }
        if (ehOProprioUsuario && !username.equals(existente.getUsername())) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio nome de usuário.");
        }

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        if (!username.equals(existente.getUsername())) {
            alteracoes.add(new AlteracaoCampo("username", existente.getUsername(), username));
        }
        if (novoPerfil != perfilAtual || !novoPerfil.name().equals(existente.getRole())) {
            alteracoes.add(new AlteracaoCampo("perfil", existente.getRole(), novoPerfil.name()));
        }
        if (trocaSenha) {
            // Nunca registrar senha ou hash: apenas o fato de que ela foi redefinida.
            alteracoes.add(new AlteracaoCampo("senha", "[OCULTA]", "[REDEFINIDA]"));
            existente.setPassword(passwordEncoder.encode(dados.password()));
        }
        existente.setUsername(username);
        existente.setRole(novoPerfil.name());

        Usuario salvo = usuarioRepository.save(existente);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO,
                    salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (existente.getUsername().equals(SegurancaUtils.usuarioAtual().username())) {
            throw new RegraNegocioException("Você não pode excluir a sua própria conta.");
        }
        if (existente.getPerfil() == Perfil.ADMIN && contarAdmins() <= 1) {
            throw new RegraNegocioException("O sistema precisa manter ao menos um administrador.");
        }
        usuarioRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_USUARIO, AuditoriaService.ENTIDADE_USUARIO, id,
                List.of(new AlteracaoCampo("username", existente.getUsername(), null),
                        new AlteracaoCampo("perfil", existente.getRole(), null)), null);
    }

    private long contarAdmins() {
        return usuarioRepository.countByRole(Perfil.ADMIN.name());
    }

    private static String normalizarUsername(String username, List<String> erros) {
        if (username == null || username.isBlank()) {
            erros.add("Informe o nome de usuário.");
            return "";
        }
        String u = username.trim().toLowerCase();
        if (!u.matches("[a-z0-9._-]{3,40}")) {
            erros.add("Nome de usuário deve ter de 3 a 40 caracteres (letras, números, ponto, hífen ou sublinhado).");
        }
        return u;
    }
}
