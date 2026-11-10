package com.umc.pediupatrao.exception;

/** Recurso inexistente. Resulta em HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
