package com.umc.pediupatrao.controller;

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
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.service.DashboardService;
import java.util.List;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API do dashboard administrativo (ADMIN e GERENTE). Somente leitura (GET).
 * Parametros comuns: de, ate (AAAA-MM-DD), status (repetido ou separado por virgula),
 * tipoEntrega (ENTREGA|RETIRADA), categoria e produtoId. Documentacao: docs/api-endpoints.md.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoVendas resumo(@RequestParam MultiValueMap<String, String> params) {
        return dashboardService.resumo(filtro(params));
    }

    @GetMapping("/vendas-por-periodo")
    public List<PontoSerie> vendasPorPeriodo(@RequestParam MultiValueMap<String, String> params) {
        FiltroDashboard filtro = filtro(params);
        return dashboardService.vendasPorPeriodo(filtro, FiltroDashboardParser.agrupamento(params, filtro));
    }

    @GetMapping("/vendas-por-hora")
    public List<VendasPorHora> vendasPorHora(@RequestParam MultiValueMap<String, String> params) {
        return dashboardService.vendasPorHora(filtro(params));
    }

    @GetMapping("/status")
    public List<SituacaoStatus> status(@RequestParam MultiValueMap<String, String> params) {
        return dashboardService.situacaoPorStatus(filtro(params));
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoRanking> produtosMaisVendidos(@RequestParam MultiValueMap<String, String> params) {
        FiltroDashboard filtro = filtro(params);
        int limite = FiltroDashboardParser.inteiro(params, "limite", 10, 1, 50);
        String ordenarPor = FiltroDashboardParser.texto(params, "ordenarPor");
        boolean porFaturamento;
        if (ordenarPor == null || ordenarPor.equalsIgnoreCase("QUANTIDADE")) {
            porFaturamento = false;
        } else if (ordenarPor.equalsIgnoreCase("FATURAMENTO")) {
            porFaturamento = true;
        } else {
            throw new ValidacaoException("ordenarPor inválido: use QUANTIDADE ou FATURAMENTO.");
        }
        return dashboardService.produtosMaisVendidos(filtro, limite, porFaturamento);
    }

    @GetMapping("/vendas-por-categoria")
    public List<VendasPorCategoria> vendasPorCategoria(@RequestParam MultiValueMap<String, String> params) {
        return dashboardService.vendasPorCategoria(filtro(params));
    }

    @GetMapping("/pedidos-recentes")
    public List<PedidoRecente> pedidosRecentes(@RequestParam MultiValueMap<String, String> params) {
        return dashboardService.pedidosRecentes(filtro(params), FiltroDashboardParser.inteiro(params, "limite", 10, 1, 50));
    }

    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return dashboardService.opcoesFiltro();
    }

    private FiltroDashboard filtro(MultiValueMap<String, String> params) {
        return FiltroDashboardParser.parse(params, dashboardService.hoje());
    }
}
