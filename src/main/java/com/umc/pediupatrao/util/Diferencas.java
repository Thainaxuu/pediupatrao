package com.umc.pediupatrao.util;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compara dois "retratos" de uma entidade e devolve apenas os campos que mudaram. */
public final class Diferencas {

    private Diferencas() {
    }

    public static List<AlteracaoCampo> comparar(Map<String, String> antes, Map<String, String> depois) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        Map<String, String> a = antes != null ? antes : Map.of();
        Map<String, String> d = depois != null ? depois : Map.of();

        LinkedHashMap<String, Boolean> campos = new LinkedHashMap<>();
        a.keySet().forEach(k -> campos.put(k, true));
        d.keySet().forEach(k -> campos.put(k, true));

        for (String campo : campos.keySet()) {
            String valorAntes = normalizar(a.get(campo));
            String valorDepois = normalizar(d.get(campo));
            if (!Objects.equals(valorAntes, valorDepois)) {
                alteracoes.add(new AlteracaoCampo(campo, valorAntes, valorDepois));
            }
        }
        return alteracoes;
    }

    /** Registro de inclusao: todos os campos preenchidos aparecem com valor anterior nulo. */
    public static List<AlteracaoCampo> inclusao(Map<String, String> depois) {
        return comparar(Map.of(), depois);
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim();
        return v.isEmpty() ? null : v;
    }
}
