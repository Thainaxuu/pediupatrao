package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;

    /** Controle de concorrencia otimista: impede que duas alteracoes simultaneas se sobrescrevam. */
    @Version
    private Long versao;

    private String clienteId;
    private String clienteNome;

    private TipoEntrega tipoEntrega;
    private String observacao;

    private List<ItemPedido> itens = new ArrayList<>();

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal percentualDesconto;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorDesconto;

    /** Valor final calculado exclusivamente no servidor. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorTotal;

    @Indexed
    private StatusPedido status;

    // Entrada
    @Indexed
    private Instant dataHoraEntrada;
    private String registradoPor;
    private String registradoPorPerfil;

    // Saida (SAIU_PARA_ENTREGA ou RETIRADO)
    private Instant dataHoraSaida;
    private String responsavelSaida;

    // Finalizacao
    private Instant dataHoraFinalizacao;
    private String responsavelFinalizacao;

    // Desconto
    private String descontoAplicadoPor;
    private Instant dataHoraDesconto;
    private String justificativaDesconto;

    // Cancelamento
    private String motivoCancelamento;
    private String canceladoPor;
    private Instant dataHoraCancelamento;

    private List<HistoricoStatus> historico = new ArrayList<>();

    // Campos da versao anterior, mantidos apenas para leitura de pedidos antigos
    private String tipoPizza;
    private String sabor;
    private Integer quantidade;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public TipoEntrega getTipoEntrega() { return tipoEntrega; }
    public void setTipoEntrega(TipoEntrega tipoEntrega) { this.tipoEntrega = tipoEntrega; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getPercentualDesconto() { return percentualDesconto; }
    public void setPercentualDesconto(BigDecimal percentualDesconto) { this.percentualDesconto = percentualDesconto; }

    public BigDecimal getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) { this.valorDesconto = valorDesconto; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public Instant getDataHoraEntrada() { return dataHoraEntrada; }
    public void setDataHoraEntrada(Instant dataHoraEntrada) { this.dataHoraEntrada = dataHoraEntrada; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public String getRegistradoPorPerfil() { return registradoPorPerfil; }
    public void setRegistradoPorPerfil(String registradoPorPerfil) { this.registradoPorPerfil = registradoPorPerfil; }

    public Instant getDataHoraSaida() { return dataHoraSaida; }
    public void setDataHoraSaida(Instant dataHoraSaida) { this.dataHoraSaida = dataHoraSaida; }

    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }

    public Instant getDataHoraFinalizacao() { return dataHoraFinalizacao; }
    public void setDataHoraFinalizacao(Instant dataHoraFinalizacao) { this.dataHoraFinalizacao = dataHoraFinalizacao; }

    public String getResponsavelFinalizacao() { return responsavelFinalizacao; }
    public void setResponsavelFinalizacao(String responsavelFinalizacao) { this.responsavelFinalizacao = responsavelFinalizacao; }

    public String getDescontoAplicadoPor() { return descontoAplicadoPor; }
    public void setDescontoAplicadoPor(String descontoAplicadoPor) { this.descontoAplicadoPor = descontoAplicadoPor; }

    public Instant getDataHoraDesconto() { return dataHoraDesconto; }
    public void setDataHoraDesconto(Instant dataHoraDesconto) { this.dataHoraDesconto = dataHoraDesconto; }

    public String getJustificativaDesconto() { return justificativaDesconto; }
    public void setJustificativaDesconto(String justificativaDesconto) { this.justificativaDesconto = justificativaDesconto; }

    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }

    public String getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(String canceladoPor) { this.canceladoPor = canceladoPor; }

    public Instant getDataHoraCancelamento() { return dataHoraCancelamento; }
    public void setDataHoraCancelamento(Instant dataHoraCancelamento) { this.dataHoraCancelamento = dataHoraCancelamento; }

    public List<HistoricoStatus> getHistorico() { return historico; }
    public void setHistorico(List<HistoricoStatus> historico) { this.historico = historico; }

    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    /** Status efetivo (pedidos antigos sem status sao tratados como RECEBIDO). */
    public StatusPedido getStatusEfetivo() {
        return status != null ? status : StatusPedido.RECEBIDO;
    }

    /** Tipo de entrega efetivo (pedidos antigos sem tipo sao tratados como ENTREGA). */
    public TipoEntrega getTipoEntregaEfetivo() {
        return tipoEntrega != null ? tipoEntrega : TipoEntrega.ENTREGA;
    }

    public List<StatusPedido> getProximosStatus() {
        return getStatusEfetivo().proximosPermitidos(getTipoEntregaEfetivo());
    }

    public boolean isLegado() {
        return itens == null || itens.isEmpty();
    }
}
