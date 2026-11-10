package com.umc.pediupatrao.exception;

import com.umc.pediupatrao.security.AcessoNegadoHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Respostas de erro padronizadas para todos os controladores REST (/api/**). */
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final AcessoNegadoHandler acessoNegadoHandler;

    public ApiExceptionHandler(AcessoNegadoHandler acessoNegadoHandler) {
        this.acessoNegadoHandler = acessoNegadoHandler;
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponse> validacao(ValidacaoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos.", ex.getErros(), req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), req);
    }

    /** Negacao vinda do @PreAuthorize dos servicos. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        acessoNegadoHandler.registrar(req, ex);
        return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para executar esta operação.", List.of(), req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> concorrencia(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT,
                "O registro foi alterado por outro usuário. Recarregue e tente novamente.", List.of(), req);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErroResponse> duplicado(DuplicateKeyException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Registro duplicado.", List.of(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido (JSON malformado ou valor não permitido).", List.of(), req);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponse> parametroInvalido(Exception ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido ou ausente.", List.of(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoPermitido(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido para este recurso.", List.of(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", List.of(), req);
    }

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, List<String> detalhes,
                                                         HttpServletRequest req) {
        ErroResponse corpo = new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, detalhes, req.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
