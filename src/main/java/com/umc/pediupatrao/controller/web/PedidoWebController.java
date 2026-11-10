package com.umc.pediupatrao.controller.web;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.security.SegurancaUtils;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pedidos")
public class PedidoWebController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final AuditoriaService auditoriaService;

    public PedidoWebController(PedidoService pedidoService, ClienteService clienteService,
                               ProdutoService produtoService, AuditoriaService auditoriaService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) StatusPedido status, Model model) {
        model.addAttribute("pedidos", pedidoService.listarPorStatus(status));
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("todosStatus", StatusPedido.values());
        model.addAttribute("content", "pedidos :: content");
        return "layout";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("produtos", produtoService.listarAtivos());
        model.addAttribute("tiposEntrega", TipoEntrega.values());
        model.addAttribute("content", "pedidos/form :: content");
        return "layout";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable String id, Model model, RedirectAttributes ra) {
        Pedido pedido;
        try {
            pedido = pedidoService.buscarPorId(id);
        } catch (RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pedidos";
        }
        model.addAttribute("pedido", pedido);
        if (SegurancaUtils.possuiPerfil("ADMIN") || SegurancaUtils.possuiPerfil("GERENTE")) {
            model.addAttribute("trilha", auditoriaService.historicoDaEntidade(AuditoriaService.ENTIDADE_PEDIDO, id));
        }
        model.addAttribute("content", "pedidos/detalhe :: content");
        return "layout";
    }
}
