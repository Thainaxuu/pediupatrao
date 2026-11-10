package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.HistoricoStatus;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.security.SegurancaUtils;
import com.umc.pediupatrao.security.UsuarioLogado;
import com.umc.pediupatrao.util.Formatos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    static final int MAX_ITENS = 30;
    static final int MAX_QUANTIDADE_ITEM = 99;
    static final int MIN_JUSTIFICATIVA_CANCELAMENTO = 10;
    static final int MIN_JUSTIFICATIVA_DESCONTO = 5;
    static final int MAX_TEXTO = 500;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProdutoRepository produtoRepository,
                         ClienteRepository clienteRepository,
                         AuditoriaService auditoriaService,
                         Clock clock) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ consultas

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByDataHoraEntradaDesc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPorStatus(StatusPedido status) {
        if (status == null) {
            return listarPedidos();
        }
        return pedidoRepository.findByStatusOrderByDataHoraEntradaDesc(status);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    // ------------------------------------------------------------------ RF02: registro

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido criarPedido(NovoPedidoRequest req) {
        List<String> erros = new ArrayList<>();
        if (req == null) {
            throw new ValidacaoException("Corpo da requisição ausente.");
        }
        if (vazio(req.clienteId())) {
            erros.add("Informe o cliente.");
        }
        if (req.itens() == null || req.itens().isEmpty()) {
            erros.add("Informe ao menos um item.");
        } else if (req.itens().size() > MAX_ITENS) {
            erros.add("O pedido pode ter no máximo " + MAX_ITENS + " itens.");
        }
        if (req.observacao() != null && req.observacao().length() > MAX_TEXTO) {
            erros.add("A observação pode ter no máximo " + MAX_TEXTO + " caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        Cliente cliente = clienteRepository.findById(req.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));

        TipoEntrega tipoEntrega = req.tipoEntrega() != null ? req.tipoEntrega() : TipoEntrega.ENTREGA;
        if (tipoEntrega == TipoEntrega.ENTREGA && vazio(cliente.getLogradouro()) && vazio(cliente.getEndereco())) {
            throw new RegraNegocioException("O cliente não possui endereço cadastrado para entrega.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        for (int i = 0; i < req.itens().size(); i++) {
            ItemPedidoRequest itemReq = req.itens().get(i);
            int posicao = i + 1;
            if (itemReq == null || vazio(itemReq.produtoId())) {
                erros.add("Item " + posicao + ": informe o produto.");
                continue;
            }
            if (itemReq.quantidade() == null || itemReq.quantidade() < 1 || itemReq.quantidade() > MAX_QUANTIDADE_ITEM) {
                erros.add("Item " + posicao + ": quantidade deve estar entre 1 e " + MAX_QUANTIDADE_ITEM + ".");
                continue;
            }
            Produto produto = produtoRepository.findById(itemReq.produtoId()).orElse(null);
            if (produto == null) {
                erros.add("Item " + posicao + ": produto não encontrado.");
                continue;
            }
            if (!produto.isDisponivel()) {
                erros.add("Item " + posicao + ": o produto '" + produto.getNome() + "' está inativo.");
                continue;
            }
            if (produto.getPreco() == null || produto.getPreco() <= 0) {
                erros.add("Item " + posicao + ": o produto '" + produto.getNome() + "' não possui preço válido.");
                continue;
            }
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_EVEN);
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), produto.getTipo(),
                    itemReq.quantidade(), preco));
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        UsuarioLogado usuario = SegurancaUtils.usuarioAtual();
        Instant agora = agora();

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoEntrega(tipoEntrega);
        pedido.setObservacao(limpar(req.observacao()));
        pedido.setItens(itens);
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        CalculadoraPedido.recalcular(pedido);
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setDataHoraEntrada(agora);
        pedido.setRegistradoPor(usuario.username());
        pedido.setRegistradoPorPerfil(usuario.perfil());
        pedido.getHistorico().add(new HistoricoStatus(null, StatusPedido.RECEBIDO,
                usuario.username(), usuario.perfil(), agora, "Entrada do pedido"));

        Pedido salvo = pedidoRepository.save(pedido);

        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        alteracoes.add(new AlteracaoCampo("status", null, StatusPedido.RECEBIDO.name()));
        alteracoes.add(new AlteracaoCampo("clienteId", null, cliente.getId()));
        alteracoes.add(new AlteracaoCampo("tipoEntrega", null, tipoEntrega.name()));
        alteracoes.add(new AlteracaoCampo("itens", null, resumoItens(itens)));
        alteracoes.add(new AlteracaoCampo("valorTotal", null, Formatos.valor(salvo.getValorTotal())));
        alteracoes.add(new AlteracaoCampo("dataHoraEntrada", null, agora.toString()));
        alteracoes.add(new AlteracaoCampo("registradoPor", null, usuario.username()));
        auditoriaService.registrar(TipoOperacao.CRIACAO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, null);
        return salvo;
    }

    // ------------------------------------------------------------------ RF03: transicoes

    @PreAuthorize("hasAnyRole('ATENDENTE','GERENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus, String observacao) {
        if (novoStatus == null) {
            throw new ValidacaoException("Informe o novo status.");
        }
        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException(
                    "O cancelamento deve ser feito pela operação de cancelamento, exclusiva do gerente.");
        }
        if (observacao != null && observacao.length() > MAX_TEXTO) {
            throw new ValidacaoException("A observação pode ter no máximo " + MAX_TEXTO + " caracteres.");
        }

        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatusEfetivo();

        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Pedido cancelado não pode ter o status alterado.");
        }
        if (atual == StatusPedido.FINALIZADO) {
            throw new RegraNegocioException("Pedido finalizado não pode ter o status alterado.");
        }
        TipoEntrega tipo = pedido.getTipoEntregaEfetivo();
        if (!atual.podeTransicionarPara(novoStatus, tipo)) {
            String permitidos = atual.proximosPermitidos(tipo).stream()
                    .map(StatusPedido::name).collect(Collectors.joining(", "));
            throw new RegraNegocioException("Transição inválida: " + atual.name() + " → " + novoStatus.name()
                    + ". Próximo status permitido: " + permitidos + ".");
        }

        UsuarioLogado usuario = SegurancaUtils.usuarioAtual();
        Instant agora = agora();
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        alteracoes.add(new AlteracaoCampo("status", atual.name(), novoStatus.name()));

        pedido.setStatus(novoStatus);
        if (novoStatus.isSaida()) {
            alteracoes.add(new AlteracaoCampo("dataHoraSaida",
                    Formatos.texto(pedido.getDataHoraSaida()), agora.toString()));
            alteracoes.add(new AlteracaoCampo("responsavelSaida",
                    pedido.getResponsavelSaida(), usuario.username()));
            pedido.setDataHoraSaida(agora);
            pedido.setResponsavelSaida(usuario.username());
        }
        if (novoStatus == StatusPedido.FINALIZADO) {
            alteracoes.add(new AlteracaoCampo("dataHoraFinalizacao", null, agora.toString()));
            pedido.setDataHoraFinalizacao(agora);
            pedido.setResponsavelFinalizacao(usuario.username());
        }
        pedido.getHistorico().add(new HistoricoStatus(atual, novoStatus, usuario.username(),
                usuario.perfil(), agora, limpar(observacao)));

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(TipoOperacao.ALTERACAO_STATUS_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, limpar(observacao));
        return salvo;
    }

    // ------------------------------------------------------------------ RF04: desconto e cancelamento

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, String justificativa) {
        List<String> erros = new ArrayList<>();
        if (percentual == null) {
            erros.add("Informe o percentual de desconto.");
        } else {
            if (percentual.compareTo(BigDecimal.ZERO) < 0) {
                erros.add("O percentual de desconto não pode ser negativo.");
            }
            if (percentual.compareTo(CalculadoraPedido.DESCONTO_MAXIMO_PERCENTUAL) > 0) {
                erros.add("O desconto é limitado a " + CalculadoraPedido.DESCONTO_MAXIMO_PERCENTUAL
                        + "% do valor do pedido.");
            }
            if (percentual.stripTrailingZeros().scale() > 2) {
                erros.add("Use no máximo duas casas decimais no percentual.");
            }
        }
        if (vazio(justificativa) || justificativa.trim().length() < MIN_JUSTIFICATIVA_DESCONTO) {
            erros.add("Informe a justificativa do desconto (mínimo " + MIN_JUSTIFICATIVA_DESCONTO + " caracteres).");
        } else if (justificativa.length() > MAX_TEXTO) {
            erros.add("A justificativa pode ter no máximo " + MAX_TEXTO + " caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }

        Pedido pedido = buscarPorId(id);
        StatusPedido status = pedido.getStatusEfetivo();
        if (!status.permiteDesconto()) {
            throw new RegraNegocioException("Desconto só pode ser aplicado antes da saída para entrega ou retirada"
                    + " e em pedidos não cancelados. Status atual: " + status.name() + ".");
        }
        if (pedido.isLegado()) {
            throw new RegraNegocioException("Pedido sem itens valorados (registro antigo) não aceita desconto.");
        }

        BigDecimal percentualAnterior = pedido.getPercentualDesconto();
        BigDecimal descontoAnterior = pedido.getValorDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();

        UsuarioLogado usuario = SegurancaUtils.usuarioAtual();
        Instant agora = agora();
        pedido.setPercentualDesconto(percentual);
        CalculadoraPedido.recalcular(pedido);
        pedido.setDescontoAplicadoPor(usuario.username());
        pedido.setDataHoraDesconto(agora);
        pedido.setJustificativaDesconto(justificativa.trim());

        Pedido salvo = pedidoRepository.save(pedido);
        List<AlteracaoCampo> alteracoes = List.of(
                new AlteracaoCampo("percentualDesconto", Formatos.valor(percentualAnterior),
                        Formatos.valor(salvo.getPercentualDesconto())),
                new AlteracaoCampo("valorDesconto", Formatos.valor(descontoAnterior),
                        Formatos.valor(salvo.getValorDesconto())),
                new AlteracaoCampo("valorTotal", Formatos.valor(totalAnterior),
                        Formatos.valor(salvo.getValorTotal())));
        auditoriaService.registrar(TipoOperacao.DESCONTO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, justificativa.trim());
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        if (vazio(justificativa) || justificativa.trim().length() < MIN_JUSTIFICATIVA_CANCELAMENTO) {
            throw new ValidacaoException("Informe a justificativa do cancelamento (mínimo "
                    + MIN_JUSTIFICATIVA_CANCELAMENTO + " caracteres).");
        }
        if (justificativa.length() > MAX_TEXTO) {
            throw new ValidacaoException("A justificativa pode ter no máximo " + MAX_TEXTO + " caracteres.");
        }

        Pedido pedido = buscarPorId(id);
        StatusPedido atual = pedido.getStatusEfetivo();
        if (atual == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O pedido já está cancelado.");
        }
        if (!atual.permiteCancelamento()) {
            throw new RegraNegocioException("Pedido não pode ser cancelado após a saída para entrega ou retirada."
                    + " Status atual: " + atual.name() + ".");
        }

        UsuarioLogado usuario = SegurancaUtils.usuarioAtual();
        Instant agora = agora();
        String motivo = justificativa.trim();

        // Itens e valores sao preservados: o cancelamento nao apaga dados historicos.
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setMotivoCancelamento(motivo);
        pedido.setCanceladoPor(usuario.username());
        pedido.setDataHoraCancelamento(agora);
        pedido.getHistorico().add(new HistoricoStatus(atual, StatusPedido.CANCELADO,
                usuario.username(), usuario.perfil(), agora, motivo));

        Pedido salvo = pedidoRepository.save(pedido);
        List<AlteracaoCampo> alteracoes = List.of(
                new AlteracaoCampo("status", atual.name(), StatusPedido.CANCELADO.name()),
                new AlteracaoCampo("canceladoPor", null, usuario.username()),
                new AlteracaoCampo("dataHoraCancelamento", null, agora.toString()));
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, AuditoriaService.ENTIDADE_PEDIDO,
                salvo.getId(), alteracoes, motivo);
        return salvo;
    }

    // ------------------------------------------------------------------ auxiliares

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    static String resumoItens(List<ItemPedido> itens) {
        return itens.stream()
                .map(i -> i.getQuantidade() + "x " + i.getNomeProduto() + " (" + Formatos.valor(i.getPrecoUnitario()) + ")")
                .collect(Collectors.joining("; "));
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpar(String s) {
        return vazio(s) ? null : s.trim();
    }
}
