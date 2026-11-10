package com.umc.pediupatrao.security;

/** Usuario autenticado na requisicao atual (nome e perfil). */
public record UsuarioLogado(String username, String perfil) {

    public static final UsuarioLogado SISTEMA = new UsuarioLogado("sistema", "SISTEMA");
    public static final UsuarioLogado ANONIMO = new UsuarioLogado("anonimo", "ANONIMO");
}
