package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/** RF03: quantidade e percentual de pedidos por status. */
public record SituacaoStatus(String status, String descricao, long quantidade, BigDecimal percentual) {
}
