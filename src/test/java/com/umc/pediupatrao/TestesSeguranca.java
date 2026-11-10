package com.umc.pediupatrao;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Utilitario para simular o usuario autenticado em testes unitarios de servico. */
public final class TestesSeguranca {

    private TestesSeguranca() {
    }

    public static void logarComo(String username, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }

    public static void deslogar() {
        SecurityContextHolder.clearContext();
    }
}
