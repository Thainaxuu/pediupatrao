package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.TestesSeguranca;
import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioServiceTest {

    private UsuarioRepository repo;
    private AuditoriaService auditoria;
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        repo = mock(UsuarioRepository.class);
        auditoria = mock(AuditoriaService.class);
        service = new UsuarioService(repo, new BCryptPasswordEncoder(4), auditoria);
        when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        TestesSeguranca.logarComo("admin", "ADMIN");
    }

    @AfterEach
    void limpar() {
        TestesSeguranca.deslogar();
    }

    private Usuario usuario(String id, String username, String role) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        u.setPassword("$2a$hash");
        when(repo.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void criaUsuarioComPerfilValidoESenhaCriptografada() {
        Usuario u = service.criar(new UsuarioRequest("Gerente02", "segredo123", "GERENTE"));
        assertEquals("gerente02", u.getUsername());
        assertEquals("GERENTE", u.getRole());
        assertNotEquals("segredo123", u.getPassword());
    }

    @Test
    void recusaPerfilInexistente() {
        assertThrows(ValidacaoException.class,
                () -> service.criar(new UsuarioRequest("fulano", "segredo123", "SUPERADMIN")));
    }

    @Test
    void administradorNaoAlteraOProprioPerfil() {
        usuario("1", "admin", "ADMIN");
        assertThrows(RegraNegocioException.class,
                () -> service.atualizar("1", new UsuarioRequest("admin", null, "ATENDENTE")));
        verify(repo, never()).save(any(Usuario.class));
    }

    @Test
    void naoRemoveOUltimoAdministrador() {
        usuario("2", "outroadmin", "ADMIN");
        when(repo.countByRole("ADMIN")).thenReturn(1L);
        assertThrows(RegraNegocioException.class,
                () -> service.atualizar("2", new UsuarioRequest("outroadmin", null, "GERENTE")));
    }

    @Test
    void administradorNaoExcluiAPropriaConta() {
        usuario("1", "admin", "ADMIN");
        assertThrows(RegraNegocioException.class, () -> service.excluir("1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void auditoriaDeTrocaDeSenhaNaoGuardaASenha() {
        usuario("3", "atendente01", "ATENDENTE");
        service.atualizar("3", new UsuarioRequest("atendente01", "novaSenha123", "ATENDENTE"));

        ArgumentCaptor<List<AlteracaoCampo>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditoria).registrar(eq(TipoOperacao.ALTERACAO_USUARIO), eq("USUARIO"), eq("3"),
                captor.capture(), isNull());
        String conteudo = captor.getValue().toString();
        assertTrue(conteudo.contains("[REDEFINIDA]"));
        assertTrue(!conteudo.contains("novaSenha123") && !conteudo.contains("$2a$"));
    }

    @Test
    void perfilLegadoUserEPromovidoSomentePorAcaoExplicitaDoAdmin() {
        usuario("4", "antigo", "USER");
        service.atualizar("4", new UsuarioRequest("antigo", null, "ATENDENTE"));
        verify(auditoria).registrar(eq(TipoOperacao.ALTERACAO_USUARIO), anyString(), eq("4"), anyList(), isNull());
    }
}
