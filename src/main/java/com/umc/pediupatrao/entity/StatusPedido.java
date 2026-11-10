package com.umc.pediupatrao.entity;

import java.util.List;

/**
 * Maquina de estados do pedido.
 *
 * RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA (entrega) ou RETIRADO (balcao) -> FINALIZADO
 *
 * CANCELADO e um estado excepcional: so pode ser alcancado pela operacao de cancelamento
 * (exclusiva do GERENTE) e apenas enquanto o pedido nao saiu para entrega/retirada.
 * FINALIZADO e CANCELADO sao estados finais: nenhuma alteracao de status e aceita depois deles.
 */
public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isFinal() {
        return this == FINALIZADO || this == CANCELADO;
    }

    /** O pedido ainda nao saiu da pizzaria. */
    public boolean isAntesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    public boolean permiteCancelamento() {
        return isAntesDaSaida();
    }

    public boolean permiteDesconto() {
        return isAntesDaSaida();
    }

    /** Indica os status de saida (registram data/hora e responsavel pela saida). */
    public boolean isSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    /** Proximos status permitidos pelo fluxo normal (sem considerar o cancelamento). */
    public List<StatusPedido> proximosPermitidos(TipoEntrega tipoEntrega) {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> tipoEntrega == TipoEntrega.RETIRADA
                    ? List.of(RETIRADO)
                    : List.of(SAIU_PARA_ENTREGA);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public boolean podeTransicionarPara(StatusPedido destino, TipoEntrega tipoEntrega) {
        return destino != null && proximosPermitidos(tipoEntrega).contains(destino);
    }

    /**
     * Converte valores gravados pela versao anterior do sistema (ex.: EM_PREPARO, ENTREGUE).
     * Valores desconhecidos sao tratados como RECEBIDO para nao quebrar a listagem.
     */
    public static StatusPedido fromLegado(String valor) {
        if (valor == null || valor.isBlank()) {
            return RECEBIDO;
        }
        String v = valor.trim().toUpperCase().replace(' ', '_');
        return switch (v) {
            case "EM_PREPARO", "PREPARANDO", "EM_PREPARACAO" -> EM_PREPARACAO;
            case "ENTREGUE", "CONCLUIDO", "FINALIZADO" -> FINALIZADO;
            case "SAIU_PARA_ENTREGA", "EM_ENTREGA", "SAIU" -> SAIU_PARA_ENTREGA;
            case "PRONTO" -> PRONTO;
            case "RETIRADO" -> RETIRADO;
            case "CANCELADO" -> CANCELADO;
            default -> RECEBIDO;
        };
    }
}
