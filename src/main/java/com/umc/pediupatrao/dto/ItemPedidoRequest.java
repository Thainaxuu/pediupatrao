package com.umc.pediupatrao.dto;

/** Item informado na criacao do pedido. O preco NAO e aceito do cliente: vem do cadastro do produto. */
public record ItemPedidoRequest(String produtoId, Integer quantidade) {
}
