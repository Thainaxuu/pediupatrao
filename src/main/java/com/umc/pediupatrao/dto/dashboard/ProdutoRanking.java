package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

/** RF04: produto no ranking de mais vendidos (pedidos cancelados nao entram). */
public record ProdutoRanking(
        int posicao,
        String produtoId,
        String nome,
        String categoria,
        long quantidade,
        BigDecimal faturamento,
        BigDecimal participacaoFaturamento) {
}
