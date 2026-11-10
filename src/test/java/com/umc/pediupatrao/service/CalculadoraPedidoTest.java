package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraPedidoTest {

    private Pedido pedidoCom(String percentual) {
        Pedido p = new Pedido();
        p.setItens(List.of(
                new ItemPedido("1", "Calabresa", "Pizza", 2, new BigDecimal("45.90")),
                new ItemPedido("2", "Refrigerante", "Bebida", 1, new BigDecimal("12.00"))));
        p.setPercentualDesconto(percentual == null ? null : new BigDecimal(percentual));
        return p;
    }

    @Test
    void calculaSubtotalETotalSemDesconto() {
        Pedido p = pedidoCom(null);
        CalculadoraPedido.recalcular(p);
        assertEquals(new BigDecimal("103.80"), p.getSubtotal());
        assertEquals(new BigDecimal("0.00"), p.getValorDesconto());
        assertEquals(new BigDecimal("103.80"), p.getValorTotal());
    }

    @Test
    void aplicaDescontoPercentualComArredondamento() {
        Pedido p = pedidoCom("15");
        CalculadoraPedido.recalcular(p);
        // 103,80 x 15% = 15,57
        assertEquals(new BigDecimal("15.57"), p.getValorDesconto());
        assertEquals(new BigDecimal("88.23"), p.getValorTotal());
    }

    @Test
    void descontoMaximoDeVintePorCento() {
        Pedido p = pedidoCom("20");
        CalculadoraPedido.recalcular(p);
        assertEquals(new BigDecimal("20.76"), p.getValorDesconto());
        assertEquals(new BigDecimal("83.04"), p.getValorTotal());
    }
}
