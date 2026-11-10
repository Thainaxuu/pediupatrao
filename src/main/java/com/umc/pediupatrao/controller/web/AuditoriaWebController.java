package com.umc.pediupatrao.controller.web;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.service.AuditoriaService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaWebController {

    private final AuditoriaService auditoriaService;

    public AuditoriaWebController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String auditoria(@RequestParam(required = false) String entidade,
                            @RequestParam(required = false) String entidadeId,
                            @RequestParam(required = false) String usuario,
                            @RequestParam(required = false) TipoOperacao operacao,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        FiltroAuditoria filtro = new FiltroAuditoria(entidade, entidadeId, usuario, operacao, de, ate);
        Page<RegistroAuditoria> registros;
        try {
            registros = auditoriaService.consultar(filtro, PageRequest.of(Math.max(page, 0), 25));
        } catch (ValidacaoException e) {
            model.addAttribute("erro", e.getMessage());
            registros = Page.empty();
        }
        model.addAttribute("registros", registros);
        model.addAttribute("filtro", filtro);
        model.addAttribute("operacoes", TipoOperacao.values());
        model.addAttribute("entidades", new String[]{AuditoriaService.ENTIDADE_PEDIDO,
                AuditoriaService.ENTIDADE_CLIENTE, AuditoriaService.ENTIDADE_PRODUTO,
                AuditoriaService.ENTIDADE_USUARIO, AuditoriaService.ENTIDADE_SISTEMA});
        model.addAttribute("content", "auditoria/lista :: content");
        return "layout";
    }
}
