package com.umc.pediupatrao.controller.web;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {

    private final UsuarioService usuarioService;

    public UsuarioWebController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("content", "usuarios/lista :: content");
        return "layout";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Usuario u = new Usuario();
        u.setRole(Perfil.ATENDENTE.name());
        return form(model, u);
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable String id, Model model, RedirectAttributes ra) {
        return usuarioService.buscarPorId(id).map(u -> {
            // Copia sem o hash da senha: o campo de senha nunca e preenchido no formulario.
            Usuario copia = new Usuario();
            copia.setId(u.getId());
            copia.setUsername(u.getUsername());
            copia.setRole(u.getPerfil().name());
            return form(model, copia);
        }).orElseGet(() -> {
            ra.addFlashAttribute("erro", "Usuário não encontrado.");
            return "redirect:/usuarios";
        });
    }

    @PostMapping("/salvar")
    public String salvar(@RequestParam String username, @RequestParam(required = false) String password,
                         @RequestParam(name = "role", required = false) String perfil,
                         RedirectAttributes ra) {
        try {
            usuarioService.criar(new UsuarioRequest(username, password, perfil));
            ra.addFlashAttribute("sucesso", "Usuário criado com sucesso!");
        } catch (ValidacaoException | RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/novo";
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/editar/{id}")
    public String atualizar(@PathVariable String id, @RequestParam String username,
                            @RequestParam(required = false) String password,
                            @RequestParam(name = "role", required = false) String perfil,
                            RedirectAttributes ra) {
        try {
            usuarioService.atualizar(id, new UsuarioRequest(username, password, perfil));
            ra.addFlashAttribute("sucesso", "Usuário atualizado com sucesso!");
        } catch (ValidacaoException | RegraNegocioException | RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/deletar/{id}")
    public String deletar(@PathVariable String id, RedirectAttributes ra) {
        try {
            usuarioService.excluir(id);
            ra.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    private String form(Model model, Usuario usuario) {
        model.addAttribute("usuario", usuario);
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("content", "usuarios/form :: content");
        return "layout";
    }
}
