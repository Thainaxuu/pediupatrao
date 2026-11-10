package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.ApiExceptionHandler;
import com.umc.pediupatrao.security.AcessoNegadoHandler;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a autorizacao no BACKEND (sem depender da interface): cada perfil so executa
 * as operacoes permitidas, inclusive quando a requisicao e feita diretamente na API.
 */
@WebMvcTest(controllers = {PedidoController.class, UsuarioController.class, AuditoriaController.class,
        ClienteController.class, ProdutoController.class, DashboardController.class})
@Import({SecurityConfig.class, AcessoNegadoHandler.class, ApiExceptionHandler.class})
class AutorizacaoApiTest {

    private static final String JUSTIFICATIVA = "{\"justificativa\":\"Cliente desistiu do pedido\"}";
    private static final String DESCONTO = "{\"percentual\":10,\"justificativa\":\"Cliente fidelidade\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private ClienteService clienteService;
    @MockitoBean private ProdutoService produtoService;
    @MockitoBean private AuditoriaService auditoriaService;
    @MockitoBean private UsuarioDetailsService usuarioDetailsService;
    @MockitoBean private DashboardService dashboardService;

    // ------------------------------------------------------------- autenticacao e CSRF

    @Test
    void apiSemLoginRetorna401() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroDeUsuarioNaoEMaisPublico() throws Exception {
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"invasor\",\"password\":\"123456\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "gerente01", roles = "GERENTE")
    void requisicaoSemTokenCsrfERecusada() throws Exception {
        mvc.perform(post("/api/pedidos/p1/cancelar").contentType(MediaType.APPLICATION_JSON).content(JUSTIFICATIVA))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- cancelamento e desconto: somente GERENTE

    @Test
    @WithMockUser(username = "gerente01", roles = "GERENTE")
    void gerenteCancelaPedido() throws Exception {
        when(pedidoService.cancelar(anyString(), anyString())).thenReturn(new Pedido());
        mvc.perform(post("/api/pedidos/p1/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(JUSTIFICATIVA)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoCancelaPedido() throws Exception {
        mvc.perform(post("/api/pedidos/p1/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(JUSTIFICATIVA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteNaoCancelaPedido() throws Exception {
        mvc.perform(post("/api/pedidos/p1/cancelar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(JUSTIFICATIVA)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoAplicaDesconto() throws Exception {
        mvc.perform(post("/api/pedidos/p1/desconto").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(DESCONTO)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteNaoAplicaDesconto() throws Exception {
        mvc.perform(post("/api/pedidos/p1/desconto").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(DESCONTO)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- pedidos

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteRegistraPedido() throws Exception {
        Pedido p = new Pedido();
        p.setId("p1");
        when(pedidoService.criarPedido(any())).thenReturn(p);
        mvc.perform(post("/api/pedidos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":\"c1\",\"tipoEntrega\":\"ENTREGA\",\"itens\":[{\"produtoId\":\"x\",\"quantidade\":1}]}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminApenasConsultaPedidos() throws Exception {
        mvc.perform(get("/api/pedidos")).andExpect(status().isOk());
        mvc.perform(post("/api/pedidos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/pedidos/p1/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_PREPARACAO\"}")).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- usuarios: somente ADMIN

    @Test
    @WithMockUser(username = "gerente01", roles = "GERENTE")
    void gerenteNaoAdministraUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"novo\",\"password\":\"123456\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCriaUsuarioERespostaNaoExpoeSenha() throws Exception {
        Usuario u = new Usuario();
        u.setId("u1");
        u.setUsername("novo");
        u.setRole("GERENTE");
        u.setPassword("$2a$10$hash");
        when(usuarioService.criar(any(UsuarioRequest.class))).thenReturn(u);
        mvc.perform(post("/api/usuarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"novo\",\"password\":\"123456\",\"perfil\":\"GERENTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("GERENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ------------------------------------------------------------- auditoria e clientes

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteNaoConsultaAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void auditoriaNaoPodeSerExcluidaPelaApi() throws Exception {
        mvc.perform(delete("/api/auditoria/a1").with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(username = "gerente01", roles = "GERENTE")
    void gerenteApenasConsultaClientes() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isOk());
        mvc.perform(post("/api/clientes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Ana\",\"telefone\":\"11999998888\"}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteNaoAlteraPrecoDeProduto() throws Exception {
        mvc.perform(put("/api/produtos/x").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Calabresa\",\"tipo\":\"Pizza\",\"preco\":1.0}")).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------- dashboard: ADMIN e GERENTE

    @Test
    @WithMockUser(username = "atendente01", roles = "ATENDENTE")
    void atendenteNaoAcessaDashboard() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gerente01", roles = "GERENTE")
    void gerenteConsultaDashboard() throws Exception {
        when(dashboardService.hoje()).thenReturn(LocalDate.of(2026, 9, 23));
        mvc.perform(get("/api/dashboard/status").param("de", "2026-09-01").param("ate", "2026-09-23"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void dashboardRecusaParametrosInvalidosCom400() throws Exception {
        when(dashboardService.hoje()).thenReturn(LocalDate.of(2026, 9, 23));
        mvc.perform(get("/api/dashboard/resumo").param("de", "2026-09-30").param("ate", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").exists());
    }
}
