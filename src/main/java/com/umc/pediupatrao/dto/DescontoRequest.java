package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

/** Desconto percentual (0 a 20). Percentual 0 remove um desconto aplicado anteriormente. */
public record DescontoRequest(BigDecimal percentual, String justificativa) {
}
