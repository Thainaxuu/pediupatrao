package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoStatus;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoria;
import com.umc.pediupatrao.dto.dashboard.VendasPorHora;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calculos do dashboard sobre a lista de pedidos ja filtrada pelo banco.
 * Classe sem dependencias de Spring ou MongoDB, para ser testada isoladamente.
 *
 * Regras adotadas:
 *  - Faturamento, ticket medio, rankings e graficos de vendas desconsideram pedidos CANCELADOS.
 *  - Contagens de pedidos (total, por status, taxa de cancelamento) consideram todos.
 *  - Pedidos sem valor (registros da versao antiga) contam como pedidos, mas somam zero.
 */
public final class DashboardCalculator {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final DateTimeFormatter ROTULO_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter ROTULO_MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private DashboardCalculator() {
    }

    // ------------------------------------------------------------------ RF01

    public static ResumoVendas resumo(List<Pedido> atual, List<Pedido> anterior) {
        long total = atual.size();
        long cancelados = atual.stream().filter(DashboardCalculator::cancelado).count();
        long validos = total - cancelados;
        long emAndamento = atual.stream()
                .filter(p -> !p.getStatusEfetivo().isFinal())
                .count();
        BigDecimal faturamento = faturamento(atual);
        BigDecimal descontos = atual.stream().filter(p -> !cancelado(p))
                .map(p -> nvl(p.getValorDesconto())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long itens = atual.stream().filter(p -> !cancelado(p))
                .flatMap(p -> itens(p).stream()).mapToLong(ItemPedido::getQuantidade).sum();

        BigDecimal ticket = validos == 0 ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_EVEN);
        BigDecimal taxaCancelamento = percentual(cancelados, total);

        List<Long> tempos = atual.stream()
                .filter(p -> p.getDataHoraEntrada() != null && p.getDataHoraSaida() != null)
                .map(p -> Duration.between(p.getDataHoraEntrada(), p.getDataHoraSaida()).toMinutes())
                .filter(m -> m >= 0)
                .toList();
        Long tempoMedio = tempos.isEmpty() ? null
                : Math.round(tempos.stream().mapToLong(Long::longValue).average().orElse(0));

        BigDecimal faturamentoAnterior = faturamento(anterior);
        long pedidosAnterior = anterior.size();

        return new ResumoVendas(
                escala(faturamento), total, validos, cancelados, emAndamento, ticket, escala(descontos), itens,
                taxaCancelamento, tempoMedio, escala(faturamentoAnterior), pedidosAnterior,
                variacao(faturamento, faturamentoAnterior),
                variacao(BigDecimal.valueOf(total), BigDecimal.valueOf(pedidosAnterior)));
    }

    // ------------------------------------------------------------------ RF02

    /** Serie temporal com todos os periodos do intervalo (periodos sem venda aparecem com zero). */
    public static List<PontoSerie> seriePorPeriodo(List<Pedido> pedidos, FiltroDashboard filtro,
                                                   Agrupamento agrupamento, ZoneId zona) {
        Map<LocalDate, BigDecimal[]> acumulado = new LinkedHashMap<>();
        LocalDate cursor = inicioDoPeriodo(filtro.de(), agrupamento);
        while (!cursor.isAfter(filtro.ate())) {
            acumulado.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = proximo(cursor, agrupamento);
        }
        for (Pedido p : pedidos) {
            if (cancelado(p) || p.getDataHoraEntrada() == null) {
                continue;
            }
            LocalDate dia = p.getDataHoraEntrada().atZone(zona).toLocalDate();
            BigDecimal[] valores = acumulado.get(inicioDoPeriodo(dia, agrupamento));
            if (valores == null) {
                continue;
            }
            valores[0] = valores[0].add(nvl(p.getValorTotal()));
            valores[1] = valores[1].add(BigDecimal.ONE);
        }
        List<PontoSerie> serie = new ArrayList<>();
        acumulado.forEach((inicio, v) -> {
            long qtd = v[1].longValue();
            BigDecimal ticket = qtd == 0 ? BigDecimal.ZERO
                    : v[0].divide(BigDecimal.valueOf(qtd), 2, RoundingMode.HALF_EVEN);
            serie.add(new PontoSerie(inicio.toString(), rotulo(inicio, agrupamento), escala(v[0]), qtd, ticket));
        });
        return serie;
    }

    /** Distribuicao por hora do dia (0 a 23): identifica horarios de pico. */
    public static List<VendasPorHora> vendasPorHora(List<Pedido> pedidos, ZoneId zona) {
        long[] quantidade = new long[24];
        BigDecimal[] valor = new BigDecimal[24];
        for (int i = 0; i < 24; i++) {
            valor[i] = BigDecimal.ZERO;
        }
        for (Pedido p : pedidos) {
            if (cancelado(p) || p.getDataHoraEntrada() == null) {
                continue;
            }
            int hora = p.getDataHoraEntrada().atZone(zona).getHour();
            quantidade[hora]++;
            valor[hora] = valor[hora].add(nvl(p.getValorTotal()));
        }
        List<VendasPorHora> lista = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            lista.add(new VendasPorHora(h, quantidade[h], escala(valor[h])));
        }
        return lista;
    }

    // ------------------------------------------------------------------ RF03

    public static List<SituacaoStatus> situacaoPorStatus(List<Pedido> pedidos) {
        Map<StatusPedido, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido s : StatusPedido.values()) {
            contagem.put(s, 0L);
        }
        pedidos.forEach(p -> contagem.merge(p.getStatusEfetivo(), 1L, Long::sum));
        long total = pedidos.size();
        List<SituacaoStatus> lista = new ArrayList<>();
        contagem.forEach((s, q) -> lista.add(new SituacaoStatus(s.name(), s.getDescricao(), q, percentual(q, total))));
        return lista;
    }

    // ------------------------------------------------------------------ RF04

    public static List<ProdutoRanking> produtosMaisVendidos(List<Pedido> pedidos, FiltroDashboard filtro,
                                                            int limite, boolean porFaturamento) {
        Map<String, Object[]> mapa = new LinkedHashMap<>();
        BigDecimal totalItens = BigDecimal.ZERO;
        for (Pedido p : pedidos) {
            if (cancelado(p)) {
                continue;
            }
            for (ItemPedido item : itens(p)) {
                if (!itemAtendeFiltro(item, filtro)) {
                    continue;
                }
                String chave = item.getProdutoId() != null ? item.getProdutoId() : "nome:" + item.getNomeProduto();
                Object[] acc = mapa.computeIfAbsent(chave, k -> new Object[]{
                        item.getProdutoId(), item.getNomeProduto(), item.getCategoria(), 0L, BigDecimal.ZERO});
                acc[1] = item.getNomeProduto(); // nome mais recente
                acc[3] = (Long) acc[3] + item.getQuantidade();
                BigDecimal subtotal = nvl(item.getSubtotal());
                acc[4] = ((BigDecimal) acc[4]).add(subtotal);
                totalItens = totalItens.add(subtotal);
            }
        }
        Comparator<Object[]> porQtd = Comparator.comparingLong(a -> (Long) a[3]);
        Comparator<Object[]> porValor = Comparator.comparing(a -> (BigDecimal) a[4]);
        Comparator<Object[]> ordem = porFaturamento ? porValor.thenComparing(porQtd) : porQtd.thenComparing(porValor);
        List<Object[]> ordenados = mapa.values().stream()
                .sorted(ordem.reversed().thenComparing(a -> Objects.toString(a[1], "")))
                .limit(limite)
                .toList();

        List<ProdutoRanking> ranking = new ArrayList<>();
        int posicao = 1;
        for (Object[] a : ordenados) {
            BigDecimal fat = (BigDecimal) a[4];
            ranking.add(new ProdutoRanking(posicao++, (String) a[0], (String) a[1], (String) a[2], (Long) a[3],
                    escala(fat), percentual(fat, totalItens)));
        }
        return ranking;
    }

    public static List<VendasPorCategoria> vendasPorCategoria(List<Pedido> pedidos, FiltroDashboard filtro) {
        Map<String, Object[]> mapa = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Pedido p : pedidos) {
            if (cancelado(p)) {
                continue;
            }
            for (ItemPedido item : itens(p)) {
                if (!itemAtendeFiltro(item, filtro)) {
                    continue;
                }
                String cat = item.getCategoria() == null || item.getCategoria().isBlank() ? "Sem categoria" : item.getCategoria();
                Object[] acc = mapa.computeIfAbsent(cat, k -> new Object[]{0L, BigDecimal.ZERO});
                acc[0] = (Long) acc[0] + item.getQuantidade();
                acc[1] = ((BigDecimal) acc[1]).add(nvl(item.getSubtotal()));
                total = total.add(nvl(item.getSubtotal()));
            }
        }
        final BigDecimal totalFinal = total;
        return mapa.entrySet().stream()
                .sorted((a, b) -> ((BigDecimal) b.getValue()[1]).compareTo((BigDecimal) a.getValue()[1]))
                .map(e -> new VendasPorCategoria(e.getKey(), (Long) e.getValue()[0], escala((BigDecimal) e.getValue()[1]),
                        percentual((BigDecimal) e.getValue()[1], totalFinal)))
                .toList();
    }

    public static List<PedidoRecente> recentes(List<Pedido> pedidos, int limite) {
        return pedidos.stream()
                .filter(p -> p.getDataHoraEntrada() != null)
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrada).reversed())
                .limit(limite)
                .map(p -> new PedidoRecente(p.getId(), p.getDataHoraEntrada(),
                        p.getClienteNome() != null ? p.getClienteNome() : p.getClienteId(),
                        itens(p).stream().mapToInt(ItemPedido::getQuantidade).sum(),
                        escala(nvl(p.getValorTotal())), p.getStatusEfetivo().name(),
                        p.getStatusEfetivo().getDescricao(), p.getTipoEntregaEfetivo().name()))
                .toList();
    }

    // ------------------------------------------------------------------ auxiliares

    static boolean itemAtendeFiltro(ItemPedido item, FiltroDashboard filtro) {
        if (filtro == null) {
            return true;
        }
        if (filtro.produtoId() != null && !filtro.produtoId().isBlank()
                && !filtro.produtoId().equals(item.getProdutoId())) {
            return false;
        }
        return filtro.categoria() == null || filtro.categoria().isBlank()
                || filtro.categoria().equalsIgnoreCase(item.getCategoria());
    }

    static LocalDate inicioDoPeriodo(LocalDate dia, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    private static LocalDate proximo(LocalDate inicio, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> inicio.plusDays(1);
            case SEMANA -> inicio.plusWeeks(1);
            case MES -> inicio.plusMonths(1);
        };
    }

    private static String rotulo(LocalDate inicio, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> inicio.format(ROTULO_DIA);
            case SEMANA -> "Sem. " + inicio.format(ROTULO_DIA);
            case MES -> inicio.format(ROTULO_MES);
        };
    }

    private static List<ItemPedido> itens(Pedido p) {
        return p.getItens() == null ? List.of() : p.getItens();
    }

    private static boolean cancelado(Pedido p) {
        return p.getStatusEfetivo() == StatusPedido.CANCELADO;
    }

    private static BigDecimal faturamento(List<Pedido> pedidos) {
        return pedidos.stream().filter(p -> !cancelado(p))
                .map(p -> nvl(p.getValorTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal escala(BigDecimal v) {
        return nvl(v).setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal percentual(long parte, long total) {
        return percentual(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }

    static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return nvl(parte).multiply(CEM).divide(total, 2, RoundingMode.HALF_EVEN);
    }

    /** Variacao percentual; null quando nao ha base de comparacao (periodo anterior zerado). */
    static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return nvl(atual).subtract(anterior).multiply(CEM).divide(anterior, 2, RoundingMode.HALF_EVEN);
    }
}
