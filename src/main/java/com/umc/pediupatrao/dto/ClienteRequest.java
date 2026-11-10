package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Cliente;

/** Campos editaveis do cliente (o id nunca vem do corpo da requisicao). */
public record ClienteRequest(
        String nome,
        String telefone,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado) {

    public static ClienteRequest de(Cliente c) {
        return new ClienteRequest(c.getNome(), c.getTelefone(), c.getCep(), c.getLogradouro(),
                c.getNumero(), c.getComplemento(), c.getBairro(), c.getCidade(), c.getEstado());
    }
}
