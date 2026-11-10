package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AtualizarStatusRequest;
import com.umc.pediupatrao.dto.CancelamentoRequest;
import com.umc.pediupatrao.dto.DescontoRequest;
import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.service.PedidoService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    public List<Pedido> listarPedidos(@RequestParam(required = false) String status) {
        return pedidoService.listarPorStatus(converterStatus(status, false));
    }

    @GetMapping("/{id}")
    public Pedido buscar(@PathVariable String id) {
        return pedidoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody NovoPedidoRequest request) {
        Pedido criado = pedidoService.criarPedido(request);
        return ResponseEntity.created(URI.create("/api/pedidos/" + criado.getId())).body(criado);
    }

    /**
     * Aceita o novo status no corpo JSON ({"status": "EM_PREPARACAO"}) ou, por compatibilidade
     * com a versao anterior, no parametro ?status=.
     */
    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id,
                                  @RequestParam(required = false) String status,
                                  @RequestBody(required = false) AtualizarStatusRequest corpo) {
        String valor = corpo != null && corpo.status() != null ? corpo.status() : status;
        String observacao = corpo != null ? corpo.observacao() : null;
        return pedidoService.atualizarStatus(id, converterStatus(valor, true), observacao);
    }

    @PostMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestBody DescontoRequest request) {
        return pedidoService.aplicarDesconto(id, request.percentual(), request.justificativa());
    }

    @PostMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable String id, @RequestBody CancelamentoRequest request) {
        return pedidoService.cancelar(id, request.justificativa());
    }

    static StatusPedido converterStatus(String valor, boolean obrigatorio) {
        if (valor == null || valor.isBlank()) {
            if (obrigatorio) {
                throw new ValidacaoException("Informe o status.");
            }
            return null;
        }
        try {
            return StatusPedido.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacaoException("Status inválido: " + valor + ".");
        }
    }
}
