package com.umc.pediupatrao.util;

/**
 * Minimizacao de dados pessoais na auditoria (LGPD): o telefone e gravado mascarado,
 * mantendo apenas os 4 ultimos digitos, suficiente para identificar que houve a troca.
 */
public final class MascaraDados {

    private MascaraDados() {
    }

    public static String telefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return null;
        }
        if (digitos.length() <= 4) {
            return "****";
        }
        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }
}
