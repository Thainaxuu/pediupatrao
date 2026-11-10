package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/** Ponto de serie temporal (dia, semana ou mes). */
public record PontoSerie(String periodo, String rotulo, BigDecimal faturamento, long pedidos, BigDecimal ticketMedio) {
}
