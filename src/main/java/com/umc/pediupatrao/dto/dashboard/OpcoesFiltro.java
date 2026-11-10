package com.umc.pediupatrao.dto.dashboard;

import java.util.List;

/** Valores disponiveis para montar os filtros da tela (RF05). */
public record OpcoesFiltro(List<Opcao> status, List<Opcao> tiposEntrega, List<String> categorias,
                           List<Opcao> produtos, List<String> agrupamentos, int periodoMaximoDias) {

    public record Opcao(String valor, String descricao) {
    }
}
