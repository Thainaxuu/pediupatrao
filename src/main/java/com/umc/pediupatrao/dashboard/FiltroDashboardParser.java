package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.exception.ValidacaoException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converte e valida os parametros de consulta do dashboard. Todos os erros encontrados
 * sao devolvidos juntos (HTTP 400) com mensagens claras para o consumidor da API.
 */
public final class FiltroDashboardParser {

    public static final int PERIODO_PADRAO_DIAS = 30;
    public static final int PERIODO_MAXIMO_DIAS = 366;

    private FiltroDashboardParser() {
    }

    public static FiltroDashboard parse(Map<String, List<String>> params, LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        LocalDate ate = data(params, "ate", erros);
        LocalDate de = data(params, "de", erros);

        if (ate == null && de == null) {
            ate = hoje;
            de = hoje.minusDays(PERIODO_PADRAO_DIAS - 1);
        } else if (ate == null) {
            ate = de.plusDays(PERIODO_PADRAO_DIAS - 1).isAfter(hoje) ? hoje : de.plusDays(PERIODO_PADRAO_DIAS - 1);
            if (ate.isBefore(de)) {
                ate = de;
            }
        } else if (de == null) {
            de = ate.minusDays(PERIODO_PADRAO_DIAS - 1);
        }

        if (de != null && ate != null) {
            if (de.isAfter(ate)) {
                erros.add("Parâmetro 'de' não pode ser posterior a 'ate'.");
            } else if (ate.toEpochDay() - de.toEpochDay() + 1 > PERIODO_MAXIMO_DIAS) {
                erros.add("O período máximo de consulta é de " + PERIODO_MAXIMO_DIAS + " dias.");
            }
        }

        List<StatusPedido> status = new ArrayList<>();
        Set<StatusPedido> vistos = new LinkedHashSet<>();
        for (String valor : valores(params, "status")) {
            try {
                vistos.add(StatusPedido.valueOf(valor.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                erros.add("Status inválido: '" + valor + "'.");
            }
        }
        status.addAll(vistos);

        TipoEntrega tipo = null;
        String tipoTexto = primeiro(params, "tipoEntrega");
        if (tipoTexto != null) {
            try {
                tipo = TipoEntrega.valueOf(tipoTexto.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                erros.add("tipoEntrega inválido: '" + tipoTexto + "'. Use ENTREGA ou RETIRADA.");
            }
        }

        String categoria = primeiro(params, "categoria");
        String produtoId = primeiro(params, "produtoId");
        if (categoria != null && categoria.length() > 60) {
            erros.add("categoria muito longa.");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        return new FiltroDashboard(de, ate, List.copyOf(status), tipo,
                categoria == null ? null : categoria.trim(), produtoId == null ? null : produtoId.trim());
    }

    public static Agrupamento agrupamento(Map<String, List<String>> params, FiltroDashboard filtro) {
        String valor = primeiro(params, "agrupamento");
        if (valor == null) {
            // Padrao inteligente: dia ate 62 dias, semana ate 180, depois mes.
            long dias = filtro.dias();
            return dias <= 62 ? Agrupamento.DIA : (dias <= 180 ? Agrupamento.SEMANA : Agrupamento.MES);
        }
        try {
            return Agrupamento.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacaoException("agrupamento inválido: '" + valor + "'. Use DIA, SEMANA ou MES.");
        }
    }

    public static int inteiro(Map<String, List<String>> params, String nome, int padrao, int min, int max) {
        String valor = primeiro(params, nome);
        if (valor == null) {
            return padrao;
        }
        try {
            int n = Integer.parseInt(valor.trim());
            if (n < min || n > max) {
                throw new ValidacaoException(nome + " deve estar entre " + min + " e " + max + ".");
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ValidacaoException(nome + " deve ser um número inteiro.");
        }
    }

    public static String texto(Map<String, List<String>> params, String nome) {
        return primeiro(params, nome);
    }

    private static LocalDate data(Map<String, List<String>> params, String nome, List<String> erros) {
        String valor = primeiro(params, nome);
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            erros.add("Parâmetro '" + nome + "' inválido: use o formato AAAA-MM-DD.");
            return null;
        }
    }

    /** Aceita parametros repetidos (?status=A&status=B) e separados por virgula (?status=A,B). */
    private static List<String> valores(Map<String, List<String>> params, String nome) {
        List<String> resultado = new ArrayList<>();
        List<String> brutos = params.get(nome);
        if (brutos == null) {
            return resultado;
        }
        for (String b : brutos) {
            if (b == null) {
                continue;
            }
            for (String parte : b.split(",")) {
                if (!parte.isBlank()) {
                    resultado.add(parte.trim());
                }
            }
        }
        return resultado;
    }

    private static String primeiro(Map<String, List<String>> params, String nome) {
        List<String> v = params.get(nome);
        if (v == null || v.isEmpty() || v.get(0) == null || v.get(0).isBlank()) {
            return null;
        }
        return v.get(0);
    }
}
