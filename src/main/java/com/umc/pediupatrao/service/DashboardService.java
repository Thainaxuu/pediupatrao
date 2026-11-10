package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dashboard.Agrupamento;
import com.umc.pediupatrao.dashboard.DashboardCalculator;
import com.umc.pediupatrao.dashboard.FiltroDashboardParser;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.PedidoRecente;
import com.umc.pediupatrao.dto.dashboard.PontoSerie;
import com.umc.pediupatrao.dto.dashboard.ProdutoRanking;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoStatus;
import com.umc.pediupatrao.dto.dashboard.VendasPorCategoria;
import com.umc.pediupatrao.dto.dashboard.VendasPorHora;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Dados do dashboard administrativo. A filtragem por periodo, status, tipo, categoria e produto
 * e feita no MongoDB (consulta indexada por dataHoraEntrada); os calculos sao feitos pelo
 * DashboardCalculator. Para o volume de uma pizzaria (milhares de pedidos por mes) essa
 * abordagem e simples e suficiente; em volume muito maior, os calculos poderiam migrar para
 * aggregation pipelines do MongoDB sem mudar o contrato da API.
 */
@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardService {

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;
    private final Clock clock;
    private final ZoneId zona;

    public DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository, Clock clock,
                            @Value("${app.zona-horaria:America/Sao_Paulo}") String zonaHoraria) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
        this.clock = clock;
        this.zona = ZoneId.of(zonaHoraria);
    }

    public LocalDate hoje() {
        return LocalDate.now(clock.withZone(zona));
    }

    public ZoneId zona() {
        return zona;
    }

    public ResumoVendas resumo(FiltroDashboard filtro) {
        return DashboardCalculator.resumo(buscar(filtro), buscar(filtro.periodoAnterior()));
    }

    public List<PontoSerie> vendasPorPeriodo(FiltroDashboard filtro, Agrupamento agrupamento) {
        return DashboardCalculator.seriePorPeriodo(buscar(filtro), filtro, agrupamento, zona);
    }

    public List<VendasPorHora> vendasPorHora(FiltroDashboard filtro) {
        return DashboardCalculator.vendasPorHora(buscar(filtro), zona);
    }

    public List<SituacaoStatus> situacaoPorStatus(FiltroDashboard filtro) {
        return DashboardCalculator.situacaoPorStatus(buscar(filtro));
    }

    public List<ProdutoRanking> produtosMaisVendidos(FiltroDashboard filtro, int limite, boolean porFaturamento) {
        return DashboardCalculator.produtosMaisVendidos(buscar(filtro), filtro, limite, porFaturamento);
    }

    public List<VendasPorCategoria> vendasPorCategoria(FiltroDashboard filtro) {
        return DashboardCalculator.vendasPorCategoria(buscar(filtro), filtro);
    }

    public List<PedidoRecente> pedidosRecentes(FiltroDashboard filtro, int limite) {
        return DashboardCalculator.recentes(buscar(filtro), limite);
    }

    public OpcoesFiltro opcoesFiltro() {
        List<Produto> produtos = produtoRepository.findAll(Sort.by("tipo", "nome"));
        List<String> categorias = produtos.stream().map(Produto::getTipo).filter(Objects::nonNull)
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().sorted().toList();
        return new OpcoesFiltro(
                Arrays.stream(StatusPedido.values()).map(s -> new OpcoesFiltro.Opcao(s.name(), s.getDescricao())).toList(),
                Arrays.stream(TipoEntrega.values()).map(t -> new OpcoesFiltro.Opcao(t.name(), t.getDescricao())).toList(),
                categorias,
                produtos.stream().map(p -> new OpcoesFiltro.Opcao(p.getId(), p.getNome())).toList(),
                Arrays.stream(Agrupamento.values()).map(Enum::name).toList(),
                FiltroDashboardParser.PERIODO_MAXIMO_DIAS);
    }

    /** Consulta ao MongoDB com todos os filtros aplicados. */
    List<Pedido> buscar(FiltroDashboard filtro) {
        return mongoTemplate.find(new Query(criterios(filtro)), Pedido.class);
    }

    Criteria criterios(FiltroDashboard filtro) {
        List<Criteria> c = new ArrayList<>();
        c.add(Criteria.where("dataHoraEntrada")
                .gte(filtro.de().atStartOfDay(zona).toInstant())
                .lt(filtro.ate().plusDays(1).atStartOfDay(zona).toInstant()));
        if (filtro.status() != null && !filtro.status().isEmpty()) {
            c.add(Criteria.where("status").in(filtro.status().stream().map(Enum::name).toList()));
        }
        if (filtro.tipoEntrega() != null) {
            c.add(Criteria.where("tipoEntrega").is(filtro.tipoEntrega().name()));
        }
        if (filtro.produtoId() != null && !filtro.produtoId().isBlank()) {
            c.add(Criteria.where("itens.produtoId").is(filtro.produtoId()));
        }
        if (filtro.categoria() != null && !filtro.categoria().isBlank()) {
            c.add(Criteria.where("itens.categoria").is(filtro.categoria()));
        }
        return new Criteria().andOperator(c.toArray(new Criteria[0]));
    }
}
