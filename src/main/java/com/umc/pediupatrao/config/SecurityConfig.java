package com.umc.pediupatrao.config;

import com.umc.pediupatrao.security.AcessoNegadoHandler;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Autorizacao em duas camadas:
 *  1. Regras por URL e metodo HTTP (abaixo).
 *  2. @PreAuthorize nos servicos (@EnableMethodSecurity), que protegem a regra mesmo que
 *     uma nova rota seja criada sem a regra de URL correspondente.
 *
 * CSRF permanece HABILITADO. Formularios Thymeleaf recebem o token automaticamente e as
 * chamadas fetch() enviam o token no cabecalho (lido das meta tags do layout).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String GERENTE = "GERENTE";
    private static final String ATENDENTE = "ATENDENTE";

    private final UsuarioDetailsService usuarioDetailsService;
    private final AcessoNegadoHandler acessoNegadoHandler;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService, AcessoNegadoHandler acessoNegadoHandler) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.acessoNegadoHandler = acessoNegadoHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Publico
                .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**",
                        "/js/**", "/images/**", "/favicon.ico").permitAll()

                // Usuarios: somente ADMIN (antes /api/usuarios era publico)
                .requestMatchers("/usuarios/**", "/api/usuarios/**", "/admin").hasRole(ADMIN)

                // Auditoria e dashboard: ADMIN e GERENTE
                .requestMatchers("/auditoria/**", "/api/auditoria/**").hasAnyRole(ADMIN, GERENTE)
                .requestMatchers("/dashboard/**", "/api/dashboard/**").hasAnyRole(ADMIN, GERENTE)

                // Pedidos
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/desconto", "/api/pedidos/*/cancelar").hasRole(GERENTE)
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole(ATENDENTE, GERENTE)
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/status").hasAnyRole(ATENDENTE, GERENTE)
                .requestMatchers(HttpMethod.GET, "/api/pedidos/**").hasAnyRole(ADMIN, GERENTE, ATENDENTE)
                .requestMatchers("/api/pedidos/**").denyAll()
                .requestMatchers("/pedidos/novo").hasAnyRole(ATENDENTE, GERENTE)
                .requestMatchers("/pedidos/**").hasAnyRole(ADMIN, GERENTE, ATENDENTE)

                // Clientes: ATENDENTE cadastra/atualiza, GERENTE consulta (e exclui sem pedidos)
                .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasAnyRole(ATENDENTE, GERENTE)
                .requestMatchers(HttpMethod.POST, "/api/clientes").hasRole(ATENDENTE)
                .requestMatchers(HttpMethod.PUT, "/api/clientes/*").hasRole(ATENDENTE)
                .requestMatchers(HttpMethod.DELETE, "/api/clientes/*").hasRole(GERENTE)
                .requestMatchers("/api/clientes/**").denyAll()
                .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole(ATENDENTE)
                .requestMatchers("/clientes/**").hasAnyRole(ATENDENTE, GERENTE)

                // Produtos: todos consultam, GERENTE altera (informacao comercial)
                .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/produtos/**").authenticated()
                .requestMatchers("/api/produtos/**").hasRole(GERENTE)

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // API sem sessao recebe 401 (e nao um redirecionamento para a tela de login)
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
                .accessDeniedHandler(acessoNegadoHandler)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.migrateSession())
            );
        // CSRF: configuracao padrao (habilitado, token por sessao).
        return http.build();
    }
}
