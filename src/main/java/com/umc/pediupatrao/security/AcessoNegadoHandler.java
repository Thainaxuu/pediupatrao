package com.umc.pediupatrao.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.pediupatrao.exception.ErroResponse;
import com.umc.pediupatrao.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * Tratamento das tentativas de acesso indevido: registra em log e na auditoria
 * (sem corpo da requisicao, senhas ou tokens) e responde 403.
 * Requisicoes da API recebem JSON; paginas recebem a pagina de erro.
 */
@Component
public class AcessoNegadoHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AcessoNegadoHandler.class);

    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    public AcessoNegadoHandler(AuditoriaService auditoriaService, ObjectMapper objectMapper) {
        this.auditoriaService = auditoriaService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        registrar(request, ex);
        String mensagem = ex instanceof CsrfException
                ? "Token CSRF ausente ou inválido. Recarregue a página e tente novamente."
                : "Você não tem permissão para executar esta operação.";

        if (isApi(request)) {
            escreverJson(response, request, mensagem, objectMapper);
        } else {
            response.sendError(HttpStatus.FORBIDDEN.value());
        }
    }

    public void registrar(HttpServletRequest request, Exception ex) {
        UsuarioLogado usuario = SegurancaUtils.usuarioAtual();
        String recurso = request.getMethod() + " " + request.getRequestURI();
        String motivo = ex instanceof CsrfException ? "CSRF" : "PERMISSAO";
        log.warn("Acesso negado [{}]: usuario={} perfil={} recurso={}", motivo, usuario.username(),
                usuario.perfil(), recurso);
        try {
            auditoriaService.registrarAcessoNegado(recurso + " (" + motivo + ")");
        } catch (RuntimeException e) {
            log.error("Falha ao registrar acesso negado na auditoria", e);
        }
    }

    public static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    static void escreverJson(HttpServletResponse response, HttpServletRequest request, String mensagem,
                             ObjectMapper mapper) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErroResponse corpo = new ErroResponse(Instant.now(), 403, "Forbidden", mensagem, List.of(),
                request.getRequestURI());
        mapper.writeValue(response.getOutputStream(), corpo);
    }
}
