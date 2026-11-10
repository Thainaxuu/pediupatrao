package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoEntrega;
import java.util.List;

/**
 * Dados aceitos para criar um pedido. Nao ha campos de valor, desconto, status ou responsavel:
 * tudo isso e definido pelo servidor.
 */
public record NovoPedidoRequest(
        String clienteId,
        TipoEntrega tipoEntrega,
        String observacao,
        List<ItemPedidoRequest> itens) {
}
