package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Usuario;

/** Representacao publica do usuario: nunca inclui a senha (nem o hash). */
public record UsuarioResponse(String id, String username, String perfil) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getPerfil().name());
    }
}
