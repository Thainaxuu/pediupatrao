package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Garante a existencia do administrador e, opcionalmente, de usuarios de demonstracao
 * (gerente01 e atendente01). Senhas nunca sao escritas no log.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Order(1)
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder,
                                   @Value("${app.seed.admin-password:teste123}") String senhaAdmin,
                                   @Value("${app.seed.usuarios-demo:true}") boolean usuariosDemo) {
        return args -> {
            repo.findByUsername("admin").ifPresentOrElse(admin -> {
                // Admin criado pela versao anterior nao tinha perfil: corrige para ADMIN.
                if (Perfil.fromEstrito(admin.getRole()) != Perfil.ADMIN) {
                    admin.setRole(Perfil.ADMIN.name());
                    repo.save(admin);
                    log.info("Perfil do usuário admin ajustado para ADMIN.");
                }
            }, () -> {
                criar(repo, encoder, "admin", senhaAdmin, Perfil.ADMIN);
                log.info("Usuário admin criado.");
            });

            if (usuariosDemo) {
                if (repo.findByUsername("gerente01").isEmpty()) {
                    criar(repo, encoder, "gerente01", "gerente123", Perfil.GERENTE);
                    log.info("Usuário de demonstração gerente01 criado.");
                }
                if (repo.findByUsername("atendente01").isEmpty()) {
                    criar(repo, encoder, "atendente01", "atendente123", Perfil.ATENDENTE);
                    log.info("Usuário de demonstração atendente01 criado.");
                }
            }
        };
    }

    private static void criar(UsuarioRepository repo, PasswordEncoder encoder, String username,
                              String senha, Perfil perfil) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(encoder.encode(senha));
        u.setRole(perfil.name());
        repo.save(u);
    }
}
