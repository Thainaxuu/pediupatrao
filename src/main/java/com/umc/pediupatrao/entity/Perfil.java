package com.umc.pediupatrao.entity;

/**
 * Perfis de acesso do sistema.
 *
 * ADMIN     administra usuarios, consulta pedidos e auditoria.
 * GERENTE   gerencia pedidos, aplica descontos, cancela pedidos e consulta auditoria.
 * ATENDENTE cadastra e atualiza clientes, registra pedidos e acompanha o andamento.
 */
public enum Perfil {
    ADMIN("Administrador"),
    GERENTE("Gerente"),
    ATENDENTE("Atendente");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Nome da authority usada pelo Spring Security (ROLE_ADMIN, ROLE_GERENTE...). */
    public String getAuthority() {
        return "ROLE_" + name();
    }

    /**
     * Converte o valor gravado no banco. Valores antigos ("USER") ou desconhecidos
     * recebem o perfil de menor privilegio (ATENDENTE), nunca um perfil maior.
     */
    public static Perfil fromLegado(String valor) {
        if (valor == null || valor.isBlank()) {
            return ATENDENTE;
        }
        try {
            return Perfil.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ATENDENTE;
        }
    }

    /** Conversao estrita, usada ao cadastrar/alterar usuarios. Retorna null se invalido. */
    public static Perfil fromEstrito(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Perfil.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
