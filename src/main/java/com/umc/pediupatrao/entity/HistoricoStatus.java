package com.umc.pediupatrao.entity;

import java.time.Instant;

/** Registro de cada mudanca de status, gravado dentro do proprio pedido. */
public class HistoricoStatus {

    private StatusPedido statusAnterior;
    private StatusPedido statusNovo;
    private String usuario;
    private String perfil;
    private Instant dataHora;
    private String observacao;

    public HistoricoStatus() {
    }

    public HistoricoStatus(StatusPedido statusAnterior, StatusPedido statusNovo, String usuario,
                           String perfil, Instant dataHora, String observacao) {
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.observacao = observacao;
    }

    public StatusPedido getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusPedido statusAnterior) { this.statusAnterior = statusAnterior; }

    public StatusPedido getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusPedido statusNovo) { this.statusNovo = statusNovo; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public Instant getDataHora() { return dataHora; }
    public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
