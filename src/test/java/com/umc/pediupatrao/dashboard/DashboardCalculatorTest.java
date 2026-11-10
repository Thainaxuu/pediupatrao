package com.umc.pediupatrao.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoStatus;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoria;
import com.umc.pediupatrao.dto.dashboard.VendasPorHora;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.CalculadoraPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardCalculatorTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final FiltroDashboard SEMANA = new FiltroDashboard(LocalDate.of(2026, 9, 21),
            LocalDate.of(2026, 9, 27), List.of(), null, null, null);

    /** Pedido com entrada em horario local de Sao Paulo. */
    private static Pedido pedido(String dataHoraLocal, StatusPedido status, Object... itens) {
        Pedido p = new Pedido();
        p.setId(dataHoraLocal);
        p.setStatus(status);
        p.setDataHoraEntrada(java.time.LocalDateTime.parse(dataHoraLocal).atZone(SP).toInstant());
        for (int i = 0; i < itens.length; i += 4) {
            p.getItens().add(new ItemPedido((String) itens[i], (String) itens[i], (String) itens[i + 1],
                    (Integer) itens[i + 2], new BigDecimal((String) itens[i + 3])));
        }
        p.setPercentualDesconto(BigDecimal.ZERO);
        CalculadoraPedido.recalcular(p);
        return p;
    }

    private final List<Pedido> pedidos = List.of(
            pedido("2026-09-21T19:10", StatusPedido.FINALIZADO, "calabresa", "Pizza", 2, "50.00"),
            pedido("2026-09-21T20:30", StatusPedido.FINALIZADO, "calabresa", "Pizza", 1, "50.00", "coca", "Bebida", 2, "10.00"),
            pedido("2026-09-23T20:05", StatusPedido.EM_PREPARACAO, "mussarela", "Pizza", 1, "40.00"),
            pedido("2026-09-23T20:45", StatusPedido.CANCELADO, "mussarela", "Pizza", 5, "40.00"));

    @Test
    void resumoDesconsideraCanceladosNoFaturamento() {
        ResumoVendas r = DashboardCalculator.resumo(pedidos,
                List.of(pedido("2026-09-15T20:00", StatusPedido.FINALIZADO, "calabresa", "Pizza", 2, "50.00")));

        assertEquals(new BigDecimal("210.00"), r.faturamento());   // 100 + 70 + 40
        assertEquals(4, r.totalPedidos());
        assertEquals(3, r.pedidosValidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(1, r.pedidosEmAndamento());
        assertEquals(new BigDecimal("70.00"), r.ticketMedio());
        assertEquals(new BigDecimal("25.00"), r.taxaCancelamento());
        assertEquals(6, r.itensVendidos());
        assertEquals(new BigDecimal("110.00"), r.variacaoFaturamento()); // 210 vs 100
    }

    @Test
    void variacaoNulaQuandoNaoHaPeriodoAnterior() {
        assertNull(DashboardCalculator.resumo(pedidos, List.of()).variacaoFaturamento());
    }

    @Test
    void serieDiariaPreencheDiasSemVenda() {
        List<PontoSerie> serie = DashboardCalculator.seriePorPeriodo(pedidos, SEMANA, Agrupamento.DIA, SP);
        assertEquals(7, serie.size());
        assertEquals(new BigDecimal("170.00"), serie.get(0).faturamento());
        assertEquals(2, serie.get(0).pedidos());
        assertEquals(0, serie.get(1).pedidos());
        assertEquals(1, serie.get(2).pedidos()); // cancelado nao entra
    }

    @Test
    void serieSemanalAgrupaNaSegundaFeira() {
        List<PontoSerie> serie = DashboardCalculator.seriePorPeriodo(pedidos, SEMANA, Agrupamento.SEMANA, SP);
        assertEquals(1, serie.size());
        assertEquals("2026-09-21", serie.get(0).periodo());
        assertEquals(3, serie.get(0).pedidos());
    }

    @Test
    void horarioDePicoUsaFusoLocal() {
        List<VendasPorHora> horas = DashboardCalculator.vendasPorHora(pedidos, SP);
        assertEquals(24, horas.size());
        assertEquals(2, horas.get(20).pedidos());
        assertEquals(1, horas.get(19).pedidos());
    }

    @Test
    void situacaoTrazTodosOsStatus() {
        List<SituacaoStatus> s = DashboardCalculator.situacaoPorStatus(pedidos);
        assertEquals(StatusPedido.values().length, s.size());
        SituacaoStatus finalizados = s.stream().filter(x -> x.status().equals("FINALIZADO")).findFirst().orElseThrow();
        assertEquals(2, finalizados.quantidade());
        assertEquals(new BigDecimal("50.00"), finalizados.percentual());
    }

    @Test
    void rankingPorQuantidadeEPorFaturamento() {
        List<ProdutoRanking> porQtd = DashboardCalculator.produtosMaisVendidos(pedidos, SEMANA, 10, false);
        assertEquals("calabresa", porQtd.get(0).produtoId());
        assertEquals(3, porQtd.get(0).quantidade());
        assertEquals("coca", porQtd.get(1).produtoId()); // 2 unidades; mussarela cancelada nao conta
        assertEquals(1, porQtd.get(2).quantidade());

        List<ProdutoRanking> porValor = DashboardCalculator.produtosMaisVendidos(pedidos, SEMANA, 2, true);
        assertEquals(2, porValor.size());
        assertEquals(new BigDecimal("150.00"), porValor.get(0).faturamento());
        assertEquals("mussarela", porValor.get(1).produtoId());
    }

    @Test
    void filtroDeCategoriaRestringeOsItens() {
        FiltroDashboard bebidas = new FiltroDashboard(SEMANA.de(), SEMANA.ate(), List.of(), null, "Bebida", null);
        List<VendasPorCategoria> cat = DashboardCalculator.vendasPorCategoria(pedidos, bebidas);
        assertEquals(1, cat.size());
        assertEquals(new BigDecimal("100.00"), cat.get(0).participacao());
    }

    @Test
    void recentesOrdenadosDoMaisNovoParaOMaisAntigo() {
        assertEquals("2026-09-23T20:45", DashboardCalculator.recentes(pedidos, 2).get(0).id());
        assertEquals(2, DashboardCalculator.recentes(pedidos, 2).size());
    }

    @Test
    void pedidosSemDataSaoIgnoradosNosGraficosTemporais() {
        Pedido semData = new Pedido();
        semData.setStatus(StatusPedido.FINALIZADO);
        semData.setDataHoraEntrada((Instant) null);
        assertEquals(0, DashboardCalculator.vendasPorHora(List.of(semData), SP).stream().mapToLong(VendasPorHora::pedidos).sum());
    }
}
