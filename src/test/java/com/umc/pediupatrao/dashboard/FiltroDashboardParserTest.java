package com.umc.pediupatrao.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntrega;
import com.umc.pediupatrao.exception.ValidacaoException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FiltroDashboardParserTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 23);

    @Test
    void semParametrosUsaUltimos30Dias() {
        FiltroDashboard f = FiltroDashboardParser.parse(Map.of(), HOJE);
        assertEquals(HOJE, f.ate());
        assertEquals(LocalDate.of(2026, 8, 25), f.de());
        assertEquals(30, f.dias());
    }

    @Test
    void interpretaStatusRepetidoOuSeparadoPorVirgula() {
        FiltroDashboard f = FiltroDashboardParser.parse(Map.of(
                "status", List.of("FINALIZADO,cancelado", "RECEBIDO"),
                "tipoEntrega", List.of("retirada")), HOJE);
        assertEquals(List.of(StatusPedido.FINALIZADO, StatusPedido.CANCELADO, StatusPedido.RECEBIDO), f.status());
        assertEquals(TipoEntrega.RETIRADA, f.tipoEntrega());
    }

    @Test
    void reuneTodosOsErrosDeValidacao() {
        ValidacaoException ex = assertThrows(ValidacaoException.class, () -> FiltroDashboardParser.parse(Map.of(
                "de", List.of("2026-13-01"), "status", List.of("XPTO"), "tipoEntrega", List.of("DRONE")), HOJE));
        assertEquals(3, ex.getErros().size());
    }

    @Test
    void recusaPeriodoInvertidoOuMuitoLongo() {
        assertThrows(ValidacaoException.class, () -> FiltroDashboardParser.parse(Map.of(
                "de", List.of("2026-09-30"), "ate", List.of("2026-09-01")), HOJE));
        assertThrows(ValidacaoException.class, () -> FiltroDashboardParser.parse(Map.of(
                "de", List.of("2024-01-01"), "ate", List.of("2026-09-01")), HOJE));
    }

    @Test
    void periodoAnteriorTemMesmaDuracao() {
        FiltroDashboard f = FiltroDashboardParser.parse(Map.of(
                "de", List.of("2026-09-21"), "ate", List.of("2026-09-27")), HOJE);
        FiltroDashboard anterior = f.periodoAnterior();
        assertEquals(LocalDate.of(2026, 9, 14), anterior.de());
        assertEquals(LocalDate.of(2026, 9, 20), anterior.ate());
    }

    @Test
    void agrupamentoAutomaticoPeloTamanhoDoPeriodo() {
        FiltroDashboard curto = FiltroDashboardParser.parse(Map.of(), HOJE);
        FiltroDashboard longo = FiltroDashboardParser.parse(Map.of("de", List.of("2026-01-01")), HOJE);
        assertEquals(Agrupamento.DIA, FiltroDashboardParser.agrupamento(Map.of(), curto));
        assertTrue(longo.dias() <= 30);
        assertThrows(ValidacaoException.class,
                () -> FiltroDashboardParser.agrupamento(Map.of("agrupamento", List.of("ANO")), curto));
    }

    @Test
    void limiteForaDaFaixaERecusado() {
        assertThrows(ValidacaoException.class,
                () -> FiltroDashboardParser.inteiro(Map.of("limite", List.of("500")), "limite", 10, 1, 50));
        assertEquals(10, FiltroDashboardParser.inteiro(Map.of(), "limite", 10, 1, 50));
    }
}
