package com.umc.pediupatrao.dto;

/** Resultado da verificacao do encadeamento de hashes da auditoria. */
public record ResultadoIntegridade(
        boolean integro,
        long registrosVerificados,
        Long primeiraSequenciaInvalida,
        String mensagem) {
}
