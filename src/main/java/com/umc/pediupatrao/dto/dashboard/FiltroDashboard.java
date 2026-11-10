package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Filtros comuns a todos os endpoints do dashboard. Periodo inclusivo (de/ate em datas locais).
 * Quando categoria ou produtoId sao informados, entram no resultado apenas os pedidos que contem
 * itens correspondentes, e os rankings/categorias consideram apenas esses itens.
 */
public record FiltroDashboard(
        LocalDate de,
        LocalDate ate,
        List<StatusPedido> status,
        TipoEntrega tipoEntrega,
        String categoria,
        String produtoId) {

    public long dias() {
        return ChronoUnit.DAYS.between(de, ate) + 1;
    }

    /** Mesmo filtro deslocado para o periodo imediatamente anterior, de mesma duracao. */
    public FiltroDashboard periodoAnterior() {
        long dias = dias();
        return new FiltroDashboard(de.minusDays(dias), de.minusDays(1), status, tipoEntrega, categoria, produtoId);
    }

    public boolean filtraItens() {
        return (categoria != null && !categoria.isBlank()) || (produtoId != null && !produtoId.isBlank());
    }
}
