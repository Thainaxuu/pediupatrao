package com.umc.pediupatrao.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso centralizado ao usuario autenticado. */
public final class SegurancaUtils {

    private SegurancaUtils() {
    }

    public static UsuarioLogado usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return UsuarioLogado.SISTEMA;
        }
        if (auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return UsuarioLogado.ANONIMO;
        }
        String perfil = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("SEM_PERFIL");
        return new UsuarioLogado(auth.getName(), perfil);
    }

    public static boolean possuiPerfil(String perfil) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        String alvo = "ROLE_" + perfil;
        return auth.getAuthorities().stream().anyMatch(a -> alvo.equals(a.getAuthority()));
    }
}
