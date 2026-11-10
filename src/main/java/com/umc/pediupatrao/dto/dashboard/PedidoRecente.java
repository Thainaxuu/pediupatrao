package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record PedidoRecente(String id, Instant dataHoraEntrada, String cliente, int itens,
                            BigDecimal valorTotal, String status, String statusDescricao, String tipoEntrega) {
}
