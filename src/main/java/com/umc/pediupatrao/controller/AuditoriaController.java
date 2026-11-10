package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.dto.PaginaResponse;
import com.umc.pediupatrao.dto.ResultadoIntegridade;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.service.AuditoriaService;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta da auditoria (ADMIN e GERENTE). Propositalmente NAO existem endpoints
 * POST, PUT ou DELETE: registros nao podem ser alterados nem excluidos pela API.
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public PaginaResponse<RegistroAuditoria> consultar(
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String entidadeId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoOperacao operacao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int tamanho = Math.min(Math.max(size, 1), 100);
        int pagina = Math.max(page, 0);
        FiltroAuditoria filtro = new FiltroAuditoria(entidade, entidadeId, usuario, operacao, de, ate);
        return PaginaResponse.de(auditoriaService.consultar(filtro, PageRequest.of(pagina, tamanho)));
    }

    @GetMapping("/integridade")
    public ResultadoIntegridade verificarIntegridade() {
        return auditoriaService.verificarIntegridade();
    }

    @GetMapping("/{id}")
    public RegistroAuditoria buscar(@PathVariable String id) {
        return auditoriaService.buscarPorId(id);
    }
}
