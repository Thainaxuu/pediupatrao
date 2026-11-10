package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/** RF01: indicadores gerais do periodo, com comparacao ao periodo anterior de mesma duracao. */
public record ResumoVendas(
        BigDecimal faturamento,
        long totalPedidos,
        long pedidosValidos,
        long pedidosCancelados,
        long pedidosEmAndamento,
        BigDecimal ticketMedio,
        BigDecimal totalDescontos,
        long itensVendidos,
        BigDecimal taxaCancelamento,
        Long tempoMedioAteSaidaMinutos,
        BigDecimal faturamentoPeriodoAnterior,
        long pedidosPeriodoAnterior,
        BigDecimal variacaoFaturamento,
        BigDecimal variacaoPedidos) {
}
