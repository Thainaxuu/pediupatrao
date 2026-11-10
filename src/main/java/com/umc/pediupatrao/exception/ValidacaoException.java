package com.umc.pediupatrao.exception;

import java.util.List;

/** Dados de entrada invalidos. Resulta em HTTP 400 com a lista de problemas encontrados. */
public class ValidacaoException extends RuntimeException {

    private final List<String> erros;

    public ValidacaoException(List<String> erros) {
        super(String.join(" ", erros));
        this.erros = List.copyOf(erros);
    }

    public ValidacaoException(String erro) {
        this(List.of(erro));
    }

    public List<String> getErros() {
        return erros;
    }
}
