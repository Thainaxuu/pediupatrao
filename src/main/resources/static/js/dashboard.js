/*
 * Dashboard administrativo: consome a API /api/dashboard/* (RF06) e trata
 * carregamento, erros, dados vazios e atualizacao automatica (RF07).
 */
(function () {
    'use strict';

    const API = '/api/dashboard';
    const INTERVALO_MS = 60000;
    const CORES_STATUS = {
        RECEBIDO: '#0d6efd', EM_PREPARACAO: '#ffc107', PRONTO: '#0dcaf0', SAIU_PARA_ENTREGA: '#343a40',
        RETIRADO: '#6c757d', FINALIZADO: '#198754', CANCELADO: '#dc3545'
    };
    const PALETA = ['#ff6b35', '#0d6efd', '#198754', '#ffc107', '#6f42c1', '#20c997', '#dc3545', '#6c757d'];

    const graficos = {};
    let temporizador = null;
    let geracao = 0; // descarta respostas antigas quando o usuario muda o filtro rapidamente

    const $ = id => document.getElementById(id);

    // ------------------------------------------------------------------ KPIs (RF01)
    const KPIS = [
        { id: 'faturamento', rotulo: 'Faturamento', icone: 'bi-cash-coin',
          valor: r => formatarMoeda(r.faturamento), variacao: r => r.variacaoFaturamento,
          detalhe: r => 'Anterior: ' + formatarMoeda(r.faturamentoPeriodoAnterior) },
        { id: 'pedidos', rotulo: 'Pedidos', icone: 'bi-bag',
          valor: r => formatarNumero(r.totalPedidos), variacao: r => r.variacaoPedidos,
          detalhe: r => formatarNumero(r.pedidosValidos) + ' válidos · ' + formatarNumero(r.itensVendidos) + ' itens' },
        { id: 'ticket', rotulo: 'Ticket médio', icone: 'bi-receipt',
          valor: r => formatarMoeda(r.ticketMedio), detalhe: () => 'Faturamento ÷ pedidos válidos' },
        { id: 'cancelamento', rotulo: 'Cancelamentos', icone: 'bi-x-octagon',
          valor: r => formatarNumero(r.taxaCancelamento, 1) + '%',
          detalhe: r => formatarNumero(r.pedidosCancelados) + ' pedido(s)' },
        { id: 'descontos', rotulo: 'Descontos', icone: 'bi-percent',
          valor: r => formatarMoeda(r.totalDescontos), detalhe: () => 'Concedidos pelo gerente' },
        { id: 'tempo', rotulo: 'Tempo até a saída', icone: 'bi-stopwatch',
          valor: r => r.tempoMedioAteSaidaMinutos == null ? '—' : r.tempoMedioAteSaidaMinutos + ' min',
          detalhe: r => formatarNumero(r.pedidosEmAndamento) + ' em andamento' }
    ];

    function montarCardsKpi() {
        $('kpis').innerHTML = KPIS.map(k => `
            <div class="col-6 col-lg-2">
                <div class="card kpi-card shadow-sm h-100"><div class="card-body">
                    <div class="kpi-rotulo"><i class="bi ${k.icone} me-1"></i>${k.rotulo}</div>
                    <div class="kpi-valor" id="kpi-${k.id}"><span class="placeholder col-8"></span></div>
                    <div class="small text-muted" id="det-${k.id}"></div>
                </div></div>
            </div>`).join('');
    }

    function renderizarKpis(r) {
        KPIS.forEach(k => {
            let html = escapeHtml(k.valor(r));
            if (k.variacao) {
                const v = k.variacao(r);
                if (v === null || v === undefined) {
                    html += ' <span class="small text-muted fw-normal" title="Sem dados no período anterior">(novo)</span>';
                } else {
                    const classe = Number(v) >= 0 ? 'positiva' : 'negativa';
                    const seta = Number(v) >= 0 ? 'bi-arrow-up' : 'bi-arrow-down';
                    html += ` <span class="small kpi-variacao ${classe}" title="Comparado ao período anterior">`
                        + `<i class="bi ${seta}"></i>${formatarNumero(Math.abs(v), 1)}%</span>`;
                }
            }
            $('kpi-' + k.id).innerHTML = html;
            $('det-' + k.id).textContent = k.detalhe ? k.detalhe(r) : '';
        });
    }

    function erroKpis(msg) {
        KPIS.forEach(k => { $('kpi-' + k.id).innerHTML = '<span class="text-danger small">erro</span>'; });
        mostrarErro(msg);
    }

    // ------------------------------------------------------------------ filtros (RF05)
    function dataISO(d) {
        const z = n => String(n).padStart(2, '0');
        return d.getFullYear() + '-' + z(d.getMonth() + 1) + '-' + z(d.getDate());
    }

    function definirPeriodo(dias, mesAtual) {
        const hoje = new Date();
        let inicio = new Date(hoje);
        if (mesAtual) {
            inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        } else {
            inicio.setDate(hoje.getDate() - (dias - 1));
        }
        $('fDe').value = dataISO(inicio);
        $('fAte').value = dataISO(hoje);
    }

    function parametros(extra) {
        const p = new URLSearchParams();
        const campos = { de: 'fDe', ate: 'fAte', status: 'fStatus', tipoEntrega: 'fTipo', categoria: 'fCategoria', produtoId: 'fProduto' };
        Object.entries(campos).forEach(([nome, id]) => { if ($(id).value) p.set(nome, $(id).value); });
        Object.entries(extra || {}).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) p.set(k, v); });
        return p.toString();
    }

    function validarFiltros() {
        const de = $('fDe').value, ate = $('fAte').value;
        if (de && ate && de > ate) {
            mostrarErro('A data inicial não pode ser posterior à data final.');
            return false;
        }
        return true;
    }

    async function carregarOpcoes() {
        try {
            const o = await apiFetch(API + '/filtros');
            const preencher = (id, itens, valor, texto) => {
                const sel = $(id);
                itens.forEach(i => {
                    const op = document.createElement('option');
                    op.value = valor(i);
                    op.textContent = texto(i);
                    sel.appendChild(op);
                });
            };
            preencher('fStatus', o.status, i => i.valor, i => i.descricao);
            preencher('fTipo', o.tiposEntrega, i => i.valor, i => i.descricao);
            preencher('fCategoria', o.categorias, i => i, i => i);
            preencher('fProduto', o.produtos, i => i.valor, i => i.descricao);
        } catch (e) {
            mostrarErro('Não foi possível carregar as opções de filtro: ' + e.message);
        }
    }

    // ------------------------------------------------------------------ graficos (RF02, RF03, RF04)
    function estado(canvasId, mensagem) {
        const el = document.querySelector(`.estado[data-para="${canvasId}"]`);
        if (el) el.textContent = mensagem || '';
        const canvas = $(canvasId);
        if (canvas) canvas.parentElement.style.display = mensagem ? 'none' : 'block';
    }

    function desenhar(id, config) {
        if (typeof Chart === 'undefined') {
            estado(id, 'Biblioteca de gráficos indisponível (verifique a conexão com a internet).');
            return;
        }
        estado(id, '');
        if (graficos[id]) {
            graficos[id].data = config.data;
            graficos[id].options = config.options;
            graficos[id].update();
        } else {
            graficos[id] = new Chart($(id), config);
        }
    }

    const tooltipMoeda = {
        callbacks: { label: ctx => ctx.dataset.label + ': ' + (ctx.dataset.moeda ? formatarMoeda(ctx.raw) : formatarNumero(ctx.raw)) }
    };

    function renderizarPeriodo(serie) {
        if (!serie.length || serie.every(p => p.pedidos === 0)) {
            estado('graficoPeriodo', 'Nenhuma venda no período selecionado.');
            return;
        }
        desenhar('graficoPeriodo', {
            type: 'bar',
            data: {
                labels: serie.map(p => p.rotulo),
                datasets: [
                    { type: 'line', label: 'Faturamento', data: serie.map(p => Number(p.faturamento)), moeda: true,
                      borderColor: '#ff6b35', backgroundColor: 'rgba(255,107,53,.15)', fill: true, tension: .3, yAxisID: 'y' },
                    { type: 'bar', label: 'Pedidos', data: serie.map(p => p.pedidos), backgroundColor: 'rgba(13,110,253,.35)', yAxisID: 'y1' }
                ]
            },
            options: {
                maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
                plugins: { tooltip: tooltipMoeda },
                scales: {
                    y: { position: 'left', ticks: { callback: v => formatarMoeda(v) } },
                    y1: { position: 'right', grid: { drawOnChartArea: false }, ticks: { precision: 0 } }
                }
            }
        });
    }

    function renderizarStatus(lista) {
        const comDados = lista.filter(s => s.quantidade > 0);
        const andamento = lista.filter(s => ['RECEBIDO', 'EM_PREPARACAO', 'PRONTO', 'SAIU_PARA_ENTREGA'].includes(s.status));
        $('emAndamento').innerHTML = andamento
            .map(s => `<span class="badge me-1 mb-1" style="background:${CORES_STATUS[s.status]}">${escapeHtml(s.descricao)}: ${s.quantidade}</span>`)
            .join('');
        if (!comDados.length) {
            estado('graficoStatus', 'Nenhum pedido no período.');
            return;
        }
        desenhar('graficoStatus', {
            type: 'doughnut',
            data: {
                labels: comDados.map(s => s.descricao),
                datasets: [{ data: comDados.map(s => s.quantidade), backgroundColor: comDados.map(s => CORES_STATUS[s.status]) }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: { callbacks: { label: ctx => {
                        const s = comDados[ctx.dataIndex];
                        return s.descricao + ': ' + s.quantidade + ' (' + formatarNumero(s.percentual, 1) + '%)';
                    } } }
                }
            }
        });
    }

    function renderizarProdutos(lista, porFaturamento) {
        if (!lista.length) {
            estado('graficoProdutos', 'Nenhum produto vendido no período.');
            return;
        }
        desenhar('graficoProdutos', {
            type: 'bar',
            data: {
                labels: lista.map(p => p.posicao + 'º ' + p.nome),
                datasets: [{
                    label: porFaturamento ? 'Faturamento' : 'Quantidade', moeda: porFaturamento,
                    data: lista.map(p => porFaturamento ? Number(p.faturamento) : p.quantidade),
                    backgroundColor: '#ff6b35'
                }]
            },
            options: {
                indexAxis: 'y', maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: ctx => {
                        const p = lista[ctx.dataIndex];
                        return p.quantidade + ' un. · ' + formatarMoeda(p.faturamento) + ' · ' + formatarNumero(p.participacaoFaturamento, 1) + '% do faturamento';
                    } } }
                },
                scales: { x: { ticks: { callback: v => porFaturamento ? formatarMoeda(v) : v } } }
            }
        });
    }

    function renderizarHora(lista) {
        const total = lista.reduce((s, h) => s + h.pedidos, 0);
        if (!total) {
            $('horaPico').textContent = '';
            estado('graficoHora', 'Nenhum pedido no período.');
            return;
        }
        const pico = lista.reduce((a, b) => (b.pedidos > a.pedidos ? b : a));
        $('horaPico').textContent = '· pico às ' + String(pico.hora).padStart(2, '0') + 'h';
        const visiveis = lista.filter(h => h.hora >= 10 || h.pedidos > 0);
        desenhar('graficoHora', {
            type: 'bar',
            data: {
                labels: visiveis.map(h => String(h.hora).padStart(2, '0') + 'h'),
                datasets: [{ label: 'Pedidos', data: visiveis.map(h => h.pedidos),
                    backgroundColor: visiveis.map(h => h.hora === pico.hora ? '#dc3545' : 'rgba(13,110,253,.6)') }]
            },
            options: { maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { ticks: { precision: 0 } } } }
        });
    }

    function renderizarCategorias(lista) {
        if (!lista.length) {
            estado('graficoCategoria', 'Sem vendas por categoria no período.');
            return;
        }
        desenhar('graficoCategoria', {
            type: 'pie',
            data: {
                labels: lista.map(c => c.categoria),
                datasets: [{ data: lista.map(c => Number(c.faturamento)), backgroundColor: lista.map((c, i) => PALETA[i % PALETA.length]) }]
            },
            options: {
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: { callbacks: { label: ctx => {
                        const c = lista[ctx.dataIndex];
                        return c.categoria + ': ' + formatarMoeda(c.faturamento) + ' (' + formatarNumero(c.participacao, 1) + '%)';
                    } } }
                }
            }
        });
    }

    function renderizarRecentes(lista) {
        const corpo = $('tabelaRecentes');
        if (!lista.length) {
            corpo.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">Nenhum pedido no período.</td></tr>';
            return;
        }
        corpo.innerHTML = lista.map(p => `
            <tr>
                <td class="ps-3 text-nowrap">${escapeHtml(new Date(p.dataHoraEntrada).toLocaleString('pt-BR'))}</td>
                <td><a href="/pedidos/${encodeURIComponent(p.id)}">${escapeHtml(p.cliente || '—')}</a></td>
                <td class="text-center">${p.itens}</td>
                <td>${p.tipoEntrega === 'RETIRADA' ? 'Retirada' : 'Entrega'}</td>
                <td class="text-end">${formatarMoeda(p.valorTotal)}</td>
                <td class="pe-3"><span class="badge" style="background:${CORES_STATUS[p.status] || '#6c757d'}">${escapeHtml(p.statusDescricao)}</span></td>
            </tr>`).join('');
    }

    // ------------------------------------------------------------------ carga (RF06, RF07)
    function mostrarErro(msg) {
        const el = $('erroFiltro');
        el.textContent = msg;
        el.style.display = 'block';
    }

    function limparErro() {
        $('erroFiltro').style.display = 'none';
    }

    async function carregarTudo() {
        if (!validarFiltros()) return;
        limparErro();
        const minhaGeracao = ++geracao;
        const ordenarPor = document.querySelector('input[name="ordenarPor"]:checked').value;
        const q = parametros();
        const tarefas = [
            ['resumo', API + '/resumo?' + q, renderizarKpis, erroKpis],
            ['periodo', API + '/vendas-por-periodo?' + parametros({ agrupamento: $('agrupamento').value }), renderizarPeriodo, m => estado('graficoPeriodo', 'Erro: ' + m)],
            ['status', API + '/status?' + q, renderizarStatus, m => estado('graficoStatus', 'Erro: ' + m)],
            ['produtos', API + '/produtos-mais-vendidos?' + parametros({ limite: 10, ordenarPor }), l => renderizarProdutos(l, ordenarPor === 'FATURAMENTO'), m => estado('graficoProdutos', 'Erro: ' + m)],
            ['hora', API + '/vendas-por-hora?' + q, renderizarHora, m => estado('graficoHora', 'Erro: ' + m)],
            ['categoria', API + '/vendas-por-categoria?' + q, renderizarCategorias, m => estado('graficoCategoria', 'Erro: ' + m)],
            ['recentes', API + '/pedidos-recentes?' + parametros({ limite: 10 }), renderizarRecentes,
                m => { $('tabelaRecentes').innerHTML = '<tr><td colspan="6" class="text-danger text-center py-3">' + escapeHtml(m) + '</td></tr>'; }]
        ];
        const resultados = await Promise.allSettled(tarefas.map(t => apiFetch(t[1])));
        if (minhaGeracao !== geracao) return; // existe uma carga mais nova em andamento

        let sessaoExpirada = false;
        resultados.forEach((r, i) => {
            const [, , sucesso, falha] = tarefas[i];
            if (r.status === 'fulfilled') {
                try { sucesso(r.value); } catch (e) { falha('falha ao exibir dados'); console.error(e); }
            } else {
                if (r.reason && r.reason.status === 401) sessaoExpirada = true;
                falha(r.reason ? r.reason.message : 'erro desconhecido');
            }
        });
        if (sessaoExpirada) {
            pararAutoAtualizacao();
            mostrarErro('Sua sessão expirou. Recarregue a página para entrar novamente.');
        }
        $('ultimaAtualizacao').textContent = new Date().toLocaleTimeString('pt-BR');
    }

    function iniciarAutoAtualizacao() {
        pararAutoAtualizacao();
        temporizador = setInterval(() => { if (!document.hidden) carregarTudo(); }, INTERVALO_MS);
    }

    function pararAutoAtualizacao() {
        if (temporizador) clearInterval(temporizador);
        temporizador = null;
    }

    // ------------------------------------------------------------------ eventos
    function iniciar() {
        montarCardsKpi();
        definirPeriodo(30);
        document.querySelectorAll('.preset').forEach(b => b.addEventListener('click', () => {
            definirPeriodo(Number(b.dataset.dias || 1), !!b.dataset.mes);
            carregarTudo();
        }));
        $('btnAplicar').addEventListener('click', carregarTudo);
        $('btnAtualizar').addEventListener('click', carregarTudo);
        $('btnLimpar').addEventListener('click', () => {
            ['fStatus', 'fTipo', 'fCategoria', 'fProduto'].forEach(id => { $(id).value = ''; });
            definirPeriodo(30);
            carregarTudo();
        });
        ['fStatus', 'fTipo', 'fCategoria', 'fProduto', 'agrupamento'].forEach(id => $(id).addEventListener('change', carregarTudo));
        document.querySelectorAll('input[name="ordenarPor"]').forEach(r => r.addEventListener('change', carregarTudo));
        $('autoAtualizar').addEventListener('change', e => e.target.checked ? iniciarAutoAtualizacao() : pararAutoAtualizacao());
        document.addEventListener('visibilitychange', () => { if (!document.hidden && temporizador) carregarTudo(); });

        carregarOpcoes();
        carregarTudo();
        iniciarAutoAtualizacao();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', iniciar);
    } else {
        iniciar();
    }
})();
