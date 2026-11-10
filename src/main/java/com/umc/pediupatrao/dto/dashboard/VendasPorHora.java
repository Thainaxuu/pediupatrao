package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record VendasPorHora(int hora, long pedidos, BigDecimal faturamento) {
}
