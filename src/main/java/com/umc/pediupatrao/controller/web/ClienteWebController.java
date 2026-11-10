package com.umc.pediupatrao.controller.web;

import com.umc.pediupatrao.dto.ClienteRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.service.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteWebController {

    private final ClienteService clienteService;

    public ClienteWebController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        model.addAttribute("content", "clientes/lista :: content");
        return "layout";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Cliente cliente, Model model, RedirectAttributes ra) {
        try {
            clienteService.criar(ClienteRequest.de(cliente));
        } catch (ValidacaoException e) {
            cliente.setId(null);
            return formComErro(model, cliente, e.getMessage());
        }
        ra.addFlashAttribute("sucesso", "Cliente criado com sucesso!");
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        return clienteService.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            model.addAttribute("content", "clientes/form :: content");
            return "layout";
        }).orElseGet(() -> {
            ra.addFlashAttribute("erro", "Cliente não encontrado.");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id, @ModelAttribute Cliente cliente, Model model,
                            RedirectAttributes ra) {
        try {
            clienteService.atualizar(id, ClienteRequest.de(cliente));
        } catch (ValidacaoException e) {
            cliente.setId(id);
            return formComErro(model, cliente, e.getMessage());
        } catch (RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/clientes";
        }
        ra.addFlashAttribute("sucesso", "Cliente atualizado com sucesso!");
        return "redirect:/clientes";
    }

    private String formComErro(Model model, Cliente cliente, String erro) {
        model.addAttribute("cliente", cliente);
        model.addAttribute("erro", erro);
        model.addAttribute("content", "clientes/form :: content");
        return "layout";
    }
}
