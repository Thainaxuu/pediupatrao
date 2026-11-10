package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega o usuario e atribui a role correspondente ao perfil gravado no banco.
 * Antes, todos recebiam "USER"; agora cada usuario recebe ROLE_ADMIN, ROLE_GERENTE ou ROLE_ATENDENTE.
 * Valores antigos ou invalidos recebem o perfil de menor privilegio (ATENDENTE).
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = username == null ? "" : username.trim().toLowerCase();
        return usuarioRepository.findByUsername(login)
                .or(() -> usuarioRepository.findByUsername(username))
                .map(usuario -> {
                    Perfil perfil = Perfil.fromLegado(usuario.getRole());
                    return User.builder()
                            .username(usuario.getUsername())
                            .password(usuario.getPassword())
                            .roles(perfil.name())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
