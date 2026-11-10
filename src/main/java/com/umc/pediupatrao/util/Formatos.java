package com.umc.pediupatrao.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Formatos {

    private Formatos() {
    }

    public static String valor(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    public static String texto(Object valor) {
        return valor == null ? null : String.valueOf(valor);
    }
}
