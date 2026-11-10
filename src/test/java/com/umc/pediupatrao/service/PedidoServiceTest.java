package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-09-23T17:35:00Z");

    private PedidoRepository pedidoRepository;
    private ProdutoRepository produtoRepository;
    private ClienteRepository clienteRepository;
    private AuditoriaService auditoriaService;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        pedidoRepository = mock(PedidoRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        auditoriaService = mock(AuditoriaService.class);
        service = new PedidoService(pedidoRepository, produtoRepository, clienteRepository, auditoriaService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId("p1");
            }
            return p;
        });
    }

    @AfterEach
    void limpar() {
        TestesSeguranca.deslogar();
    }

    private Produto produto(String id, String nome, double preco, Boolean ativo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setTipo("Pizza");
        p.setPreco(preco);
        p.setAtivo(ativo);
        return p;
    }

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId("c1");
        c.setNome("Maria");
        c.setLogradouro("Rua das Flores");
        return c;
    }

    private Pedido pedidoNoStatus(StatusPedido status, TipoEntrega tipo) {
        Pedido p = new Pedido();
        p.setId("p1");
        p.setStatus(status);
        p.setTipoEntrega(tipo);
        p.setItens(new ArrayList<>(List.of(new ItemPedido("x", "Calabresa", "Pizza", 2, new BigDecimal("50.00")))));
        p.setPercentualDesconto(BigDecimal.ZERO);
        CalculadoraPedido.recalcular(p);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(p));
        return p;
    }

    // ------------------------------------------------------------- RF02

    @Test
    void criaPedidoCalculandoTotalNoServidorERegistrandoResponsavel() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        when(produtoRepository.findById("pz")).thenReturn(Optional.of(produto("pz", "Calabresa", 45.90, true)));
        when(produtoRepository.findById("rf")).thenReturn(Optional.of(produto("rf", "Refrigerante", 12.0, null)));

        Pedido pedido = service.criarPedido(new NovoPedidoRequest("c1", TipoEntrega.ENTREGA, null,
                List.of(new ItemPedidoRequest("pz", 2), new ItemPedidoRequest("rf", 1))));

        assertEquals(new BigDecimal("103.80"), pedido.getValorTotal());
        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        assertEquals("atendente01", pedido.getRegistradoPor());
        assertEquals(AGORA, pedido.getDataHoraEntrada());
        assertEquals("Maria", pedido.getClienteNome());
        assertEquals(1, pedido.getHistorico().size());
        verify(auditoriaService).registrar(eq(TipoOperacao.CRIACAO_PEDIDO), eq("PEDIDO"), eq("p1"), anyList(), isNull());
    }

    @Test
    void naoAceitaProdutoInativo() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        when(produtoRepository.findById("pz")).thenReturn(Optional.of(produto("pz", "Calabresa", 45.90, false)));

        assertThrows(ValidacaoException.class, () -> service.criarPedido(new NovoPedidoRequest("c1",
                TipoEntrega.ENTREGA, null, List.of(new ItemPedidoRequest("pz", 1)))));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void naoAceitaPedidoSemItensOuQuantidadeInvalida() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        assertThrows(ValidacaoException.class,
                () -> service.criarPedido(new NovoPedidoRequest("c1", TipoEntrega.ENTREGA, null, List.of())));

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
        assertThrows(ValidacaoException.class, () -> service.criarPedido(new NovoPedidoRequest("c1",
                TipoEntrega.ENTREGA, null, List.of(new ItemPedidoRequest("pz", 0)))));
    }

    // ------------------------------------------------------------- RF03

    @Test
    void avancaStatusSeguindoOFluxo() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        pedidoNoStatus(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);

        Pedido p = service.atualizarStatus("p1", StatusPedido.EM_PREPARACAO, null);

        assertEquals(StatusPedido.EM_PREPARACAO, p.getStatus());
        verify(auditoriaService).registrar(eq(TipoOperacao.ALTERACAO_STATUS_PEDIDO), eq("PEDIDO"), eq("p1"),
                anyList(), isNull());
    }

    @Test
    void bloqueiaFinalizarPedidoEmPreparacao() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        pedidoNoStatus(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> service.atualizarStatus("p1", StatusPedido.FINALIZADO, null));
        assertTrue(ex.getMessage().contains("Transição inválida"));
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void saidaRegistraDataHoraEResponsavel() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        pedidoNoStatus(StatusPedido.PRONTO, TipoEntrega.ENTREGA);

        Pedido p = service.atualizarStatus("p1", StatusPedido.SAIU_PARA_ENTREGA, "Motoboy João");

        assertEquals(AGORA, p.getDataHoraSaida());
        assertEquals("atendente01", p.getResponsavelSaida());
    }

    @Test
    void retiradaSomenteParaPedidoDeBalcao() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        pedidoNoStatus(StatusPedido.PRONTO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.atualizarStatus("p1", StatusPedido.RETIRADO, null));
    }

    @Test
    void naoAlteraStatusDePedidoCancelado() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.CANCELADO, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class,
                () -> service.atualizarStatus("p1", StatusPedido.EM_PREPARACAO, null));
    }

    @Test
    void statusCanceladoNaoPodeSerDefinidoPelaTransicaoComum() {
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
        assertThrows(RegraNegocioException.class, () -> service.atualizarStatus("p1", StatusPedido.CANCELADO, null));
    }

    // ------------------------------------------------------------- RF04

    @Test
    void cancelaComJustificativaPreservandoValores() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA);

        Pedido p = service.cancelar("p1", "Cliente desistiu por telefone");

        assertEquals(StatusPedido.CANCELADO, p.getStatus());
        assertEquals("gerente01", p.getCanceladoPor());
        assertEquals(new BigDecimal("100.00"), p.getValorTotal());
        assertNotNull(p.getDataHoraCancelamento());
        verify(auditoriaService).registrar(eq(TipoOperacao.CANCELAMENTO_PEDIDO), eq("PEDIDO"), eq("p1"),
                anyList(), eq("Cliente desistiu por telefone"));
    }

    @Test
    void cancelamentoExigeJustificativa() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        assertThrows(ValidacaoException.class, () -> service.cancelar("p1", "curta"));
    }

    @Test
    void naoCancelaDepoisDaSaida() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.SAIU_PARA_ENTREGA, TipoEntrega.ENTREGA);
        assertThrows(RegraNegocioException.class, () -> service.cancelar("p1", "Cliente desistiu por telefone"));
    }

    @Test
    void aplicaDescontoDentroDoLimite() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);

        Pedido p = service.aplicarDesconto("p1", new BigDecimal("10"), "Cliente fidelidade");

        assertEquals(new BigDecimal("10.00"), p.getValorDesconto());
        assertEquals(new BigDecimal("90.00"), p.getValorTotal());
        assertEquals("gerente01", p.getDescontoAplicadoPor());
        verify(auditoriaService).registrar(eq(TipoOperacao.DESCONTO_PEDIDO), eq("PEDIDO"), eq("p1"),
                anyList(), anyString());
    }

    @Test
    void recusaDescontoAcimaDeVintePorCento() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA);
        assertThrows(ValidacaoException.class,
                () -> service.aplicarDesconto("p1", new BigDecimal("20.01"), "Cliente fidelidade"));
    }

    @Test
    void recusaDescontoDepoisDaSaida() {
        TestesSeguranca.logarComo("gerente01", "GERENTE");
        pedidoNoStatus(StatusPedido.RETIRADO, TipoEntrega.RETIRADA);
        assertThrows(RegraNegocioException.class,
                () -> service.aplicarDesconto("p1", new BigDecimal("5"), "Cliente fidelidade"));
    }
}
