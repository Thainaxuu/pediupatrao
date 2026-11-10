package com.umc.pediupatrao.exception;

/** Violacao de regra de negocio (ex.: transicao de status invalida). Resulta em HTTP 422. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
