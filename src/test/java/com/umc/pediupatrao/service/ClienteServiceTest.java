package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.TestesSeguranca;
import com.umc.pediupatrao.dto.ClienteRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ValidacaoException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClienteServiceTest {

    private ClienteRepository clienteRepository;
    private PedidoRepository pedidoRepository;
    private AuditoriaService auditoria;
    private ClienteService service;

    @BeforeEach
    void preparar() {
        clienteRepository = mock(ClienteRepository.class);
        pedidoRepository = mock(PedidoRepository.class);
        auditoria = mock(AuditoriaService.class);
        service = new ClienteService(clienteRepository, pedidoRepository, auditoria);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
    }

    @AfterEach
    void limpar() {
        TestesSeguranca.deslogar();
    }

    @Test
    @SuppressWarnings("unchecked")
    void alteracaoDeEnderecoETelefoneEAuditadaComValoresAnterioresENovos() {
        Cliente c = new Cliente();
        c.setId("c1");
        c.setNome("Maria");
        c.setTelefone("(11) 98765-4321");
        c.setLogradouro("Rua das Flores");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(c));

        service.atualizar("c1", new ClienteRequest("Maria", "(11) 91234-0000", null, "Av. Brasil",
                null, null, null, null, null));

        ArgumentCaptor<List<AlteracaoCampo>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditoria).registrar(eq(TipoOperacao.ALTERACAO_CLIENTE), eq("CLIENTE"), eq("c1"),
                captor.capture(), isNull());
        List<AlteracaoCampo> alteracoes = captor.getValue();
        assertEquals(2, alteracoes.size());
        assertEquals("telefone", alteracoes.get(0).getCampo());
        assertEquals("*******4321", alteracoes.get(0).getValorAnterior());
        assertEquals("*******0000", alteracoes.get(0).getValorNovo());
        assertEquals("logradouro", alteracoes.get(1).getCampo());
        assertEquals("Rua das Flores", alteracoes.get(1).getValorAnterior());
        assertEquals("Av. Brasil", alteracoes.get(1).getValorNovo());
    }

    @Test
    void semMudancaNaoGeraRegistroDeAuditoria() {
        Cliente c = new Cliente();
        c.setId("c1");
        c.setNome("Maria");
        c.setTelefone("11987654321");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(c));

        service.atualizar("c1", new ClienteRequest("Maria", "11987654321", null, null, null, null, null, null, null));

        verify(auditoria, never()).registrar(any(TipoOperacao.class), anyString(), anyString(), anyList(), any());
    }

    @Test
    void validaTelefone() {
        assertThrows(ValidacaoException.class, () -> service.criar(new ClienteRequest("Maria", "123",
                null, null, null, null, null, null, null)));
    }

    @Test
    void naoExcluiClienteComPedidos() {
        Cliente c = new Cliente();
        c.setId("c1");
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(c));
        when(pedidoRepository.existsByClienteId("c1")).thenReturn(true);
        assertThrows(RegraNegocioException.class, () -> service.excluir("c1"));
    }
}
