package com.umc.pediupatrao.exception;

import java.time.Instant;
import java.util.List;

/** Corpo padrao das respostas de erro da API. */
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        List<String> detalhes,
        String path) {
}
