package com.umc.pediupatrao.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Formatacao usada nos templates: ${@fmt.dataHora(instante)} e ${@fmt.moeda(valor)}. */
@Component("fmt")
public class FormatadorView {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private final DateTimeFormatter formatoDataHora;

    public FormatadorView(@Value("${app.zona-horaria:America/Sao_Paulo}") String zona) {
        this.formatoDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of(zona));
    }

    public String dataHora(Instant instante) {
        return instante == null ? "—" : formatoDataHora.format(instante);
    }

    public String moeda(BigDecimal valor) {
        if (valor == null) {
            return "—";
        }
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor.setScale(2, RoundingMode.HALF_EVEN));
    }

    public String moedaDouble(Double valor) {
        return valor == null ? "—" : moeda(BigDecimal.valueOf(valor));
    }

    public String percentual(BigDecimal valor) {
        if (valor == null) {
            return "0%";
        }
        return valor.stripTrailingZeros().toPlainString() + "%";
    }
}
