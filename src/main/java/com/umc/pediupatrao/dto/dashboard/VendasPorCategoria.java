package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record VendasPorCategoria(String categoria, long quantidade, BigDecimal faturamento, BigDecimal participacao) {
}
