package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoOperacao;
import java.time.LocalDate;

public record FiltroAuditoria(
        String entidade,
        String entidadeId,
        String usuario,
        TipoOperacao operacao,
        LocalDate de,
        LocalDate ate) {

    public static FiltroAuditoria vazio() {
        return new FiltroAuditoria(null, null, null, null, null, null);
    }
}
