package com.umc.pediupatrao.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoCompletoDeEntrega() {
        assertTrue(StatusPedido.RECEBIDO.podeTransicionarPara(StatusPedido.EM_PREPARACAO, TipoEntrega.ENTREGA));
        assertTrue(StatusPedido.EM_PREPARACAO.podeTransicionarPara(StatusPedido.PRONTO, TipoEntrega.ENTREGA));
        assertTrue(StatusPedido.PRONTO.podeTransicionarPara(StatusPedido.SAIU_PARA_ENTREGA, TipoEntrega.ENTREGA));
        assertTrue(StatusPedido.SAIU_PARA_ENTREGA.podeTransicionarPara(StatusPedido.FINALIZADO, TipoEntrega.ENTREGA));
    }

    @Test
    void fluxoCompletoDeRetirada() {
        assertEquals(List.of(StatusPedido.RETIRADO), StatusPedido.PRONTO.proximosPermitidos(TipoEntrega.RETIRADA));
        assertTrue(StatusPedido.RETIRADO.podeTransicionarPara(StatusPedido.FINALIZADO, TipoEntrega.RETIRADA));
        assertFalse(StatusPedido.PRONTO.podeTransicionarPara(StatusPedido.SAIU_PARA_ENTREGA, TipoEntrega.RETIRADA));
    }

    @Test
    void naoPermitePularOuVoltarEtapas() {
        assertFalse(StatusPedido.EM_PREPARACAO.podeTransicionarPara(StatusPedido.FINALIZADO, TipoEntrega.ENTREGA));
        assertFalse(StatusPedido.RECEBIDO.podeTransicionarPara(StatusPedido.PRONTO, TipoEntrega.ENTREGA));
        assertFalse(StatusPedido.PRONTO.podeTransicionarPara(StatusPedido.RECEBIDO, TipoEntrega.ENTREGA));
    }

    @Test
    void estadosFinaisNaoTemProximos() {
        assertTrue(StatusPedido.CANCELADO.proximosPermitidos(TipoEntrega.ENTREGA).isEmpty());
        assertTrue(StatusPedido.FINALIZADO.proximosPermitidos(TipoEntrega.ENTREGA).isEmpty());
        assertTrue(StatusPedido.CANCELADO.isFinal());
    }

    @Test
    void cancelamentoEDescontoSomenteAntesDaSaida() {
        assertTrue(StatusPedido.RECEBIDO.permiteCancelamento());
        assertTrue(StatusPedido.PRONTO.permiteDesconto());
        assertFalse(StatusPedido.SAIU_PARA_ENTREGA.permiteCancelamento());
        assertFalse(StatusPedido.RETIRADO.permiteDesconto());
        assertFalse(StatusPedido.CANCELADO.permiteDesconto());
    }

    @Test
    void converteStatusDaVersaoAnterior() {
        assertEquals(StatusPedido.EM_PREPARACAO, StatusPedido.fromLegado("EM_PREPARO"));
        assertEquals(StatusPedido.FINALIZADO, StatusPedido.fromLegado("ENTREGUE"));
        assertEquals(StatusPedido.RECEBIDO, StatusPedido.fromLegado("qualquer"));
        assertEquals(StatusPedido.RECEBIDO, StatusPedido.fromLegado(null));
    }

    @Test
    void perfilLegadoRecebeMenorPrivilegio() {
        assertEquals(Perfil.ATENDENTE, Perfil.fromLegado("USER"));
        assertEquals(Perfil.ATENDENTE, Perfil.fromLegado(null));
        assertEquals(Perfil.GERENTE, Perfil.fromLegado("gerente"));
        assertNull(Perfil.fromEstrito("SUPERADMIN"));
    }
}
