package com.umc.pediupatrao.dto;

/** Dados para criar/alterar usuario. password vazio na alteracao mantem a senha atual. */
public record UsuarioRequest(String username, String password, String perfil) {
}
