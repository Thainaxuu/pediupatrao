package com.umc.pediupatrao.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento de auditoria. Os registros sao somente inclusao: nao existe operacao de alteracao
 * ou exclusao no repositorio, no servico ou na API. Cada registro guarda o hash do registro
 * anterior (encadeamento), o que permite detectar alteracoes feitas diretamente no banco.
 */
@Document(collection = "auditoria")
@CompoundIndex(name = "entidade_id_idx", def = "{'entidade': 1, 'entidadeId': 1}")
public class RegistroAuditoria {

    @Id
    private String id;

    @Indexed(unique = true)
    private long sequencia;

    @Indexed
    private Instant dataHora;

    @Indexed
    private String usuario;
    private String perfil;

    private TipoOperacao operacao;
    private String entidade;
    private String entidadeId;

    private List<AlteracaoCampo> alteracoes = new ArrayList<>();
    private String justificativa;

    private String hashAnterior;
    private String hash;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getSequencia() { return sequencia; }
    public void setSequencia(long sequencia) { this.sequencia = sequencia; }

    public Instant getDataHora() { return dataHora; }
    public void setDataHora(Instant dataHora) { this.dataHora = dataHora; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public TipoOperacao getOperacao() { return operacao; }
    public void setOperacao(TipoOperacao operacao) { this.operacao = operacao; }

    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }

    public String getEntidadeId() { return entidadeId; }
    public void setEntidadeId(String entidadeId) { this.entidadeId = entidadeId; }

    public List<AlteracaoCampo> getAlteracoes() { return alteracoes; }
    public void setAlteracoes(List<AlteracaoCampo> alteracoes) { this.alteracoes = alteracoes; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    public String getHashAnterior() { return hashAnterior; }
    public void setHashAnterior(String hashAnterior) { this.hashAnterior = hashAnterior; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
