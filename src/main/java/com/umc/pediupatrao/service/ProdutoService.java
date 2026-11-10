package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ProdutoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.util.Diferencas;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public ProdutoService(ProdutoRepository produtoRepository, AuditoriaService auditoriaService) {
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll(Sort.by("tipo", "nome"));
    }

    public List<Produto> listarAtivos() {
        return listarProdutos().stream().filter(Produto::isDisponivel).toList();
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto criar(ProdutoRequest dados) {
        validar(dados);
        Produto p = new Produto();
        aplicar(p, dados);
        Produto salvo = produtoRepository.save(p);
        auditoriaService.registrar(TipoOperacao.CRIACAO_PRODUTO, AuditoriaService.ENTIDADE_PRODUTO,
                salvo.getId(), Diferencas.inclusao(retrato(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto atualizar(String id, ProdutoRequest dados) {
        validar(dados);
        Produto p = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        Map<String, String> antes = retrato(p);
        aplicar(p, dados);
        List<AlteracaoCampo> alteracoes = Diferencas.comparar(antes, retrato(p));
        Produto salvo = produtoRepository.save(p);
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_PRODUTO, AuditoriaService.ENTIDADE_PRODUTO,
                    salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    /** Pedidos guardam copia de nome e preco dos itens, entao excluir o produto nao afeta o historico. */
    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        Produto p = produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        Map<String, String> antes = retrato(p);
        produtoRepository.deleteById(id);
        auditoriaService.registrar(TipoOperacao.EXCLUSAO_PRODUTO, AuditoriaService.ENTIDADE_PRODUTO,
                id, Diferencas.comparar(antes, Map.of()), null);
    }

    private void validar(ProdutoRequest d) {
        List<String> erros = new ArrayList<>();
        if (d == null) {
            throw new ValidacaoException("Dados do produto ausentes.");
        }
        if (d.nome() == null || d.nome().isBlank()) {
            erros.add("Informe o nome do produto.");
        }
        if (d.tipo() == null || d.tipo().isBlank()) {
            erros.add("Informe o tipo do produto.");
        }
        if (d.preco() == null || d.preco() <= 0) {
            erros.add("Informe um preço maior que zero.");
        } else if (d.preco() > 10000) {
            erros.add("Preço acima do limite permitido.");
        }
        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
    }

    private void aplicar(Produto p, ProdutoRequest d) {
        p.setNome(d.nome().trim());
        p.setTipo(d.tipo().trim());
        p.setPreco(BigDecimal.valueOf(d.preco()).setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue());
        p.setAtivo(d.ativo() == null || d.ativo());
    }

    private static Map<String, String> retrato(Produto p) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("nome", p.getNome());
        m.put("tipo", p.getTipo());
        m.put("preco", p.getPreco() == null ? null
                : BigDecimal.valueOf(p.getPreco()).setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString());
        m.put("ativo", String.valueOf(p.isDisponivel()));
        return m;
    }
}
