package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Calculo dos valores do pedido. Toda a matematica financeira fica aqui, no servidor. */
public final class CalculadoraPedido {

    public static final BigDecimal DESCONTO_MAXIMO_PERCENTUAL = new BigDecimal("20");
    private static final BigDecimal CEM = new BigDecimal("100");

    private CalculadoraPedido() {
    }

    public static void recalcular(Pedido pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                BigDecimal itemSubtotal = item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade()))
                        .setScale(2, RoundingMode.HALF_EVEN);
                item.setSubtotal(itemSubtotal);
                subtotal = subtotal.add(itemSubtotal);
            }
        }
        BigDecimal percentual = pedido.getPercentualDesconto() != null
                ? pedido.getPercentualDesconto() : BigDecimal.ZERO;
        BigDecimal desconto = subtotal.multiply(percentual)
                .divide(CEM, 2, RoundingMode.HALF_EVEN);

        pedido.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_EVEN));
        pedido.setPercentualDesconto(percentual.setScale(2, RoundingMode.HALF_EVEN));
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(subtotal.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN));
    }
}
