package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.TestesSeguranca;
import com.umc.pediupatrao.dto.ResultadoIntegridade;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

class AuditoriaServiceTest {

    private final List<RegistroAuditoria> banco = new ArrayList<>();
    private AuditoriaService service;

    @BeforeEach
    void preparar() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        AuditoriaRepository repo = mock(AuditoriaRepository.class);
        when(mongo.insert(any(RegistroAuditoria.class))).thenAnswer(inv -> {
            RegistroAuditoria r = inv.getArgument(0);
            banco.add(r);
            return r;
        });
        when(repo.findTopByOrderBySequenciaDesc()).thenAnswer(inv ->
                banco.isEmpty() ? Optional.empty() : Optional.of(banco.get(banco.size() - 1)));
        when(repo.findAllByOrderBySequenciaAsc()).thenAnswer(inv -> new ArrayList<>(banco));
        service = new AuditoriaService(mongo, repo, Clock.fixed(Instant.parse("2026-09-23T17:35:00.123456Z"),
                ZoneOffset.UTC), "America/Sao_Paulo");
        TestesSeguranca.logarComo("atendente01", "ATENDENTE");
    }

    @AfterEach
    void limpar() {
        TestesSeguranca.deslogar();
    }

    @Test
    void registraUsuarioPerfilDataHoraEEncadeamento() {
        RegistroAuditoria r1 = service.registrar(TipoOperacao.ALTERACAO_CLIENTE, "CLIENTE", "c1",
                List.of(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil")), null);
        RegistroAuditoria r2 = service.registrar(TipoOperacao.CRIACAO_PEDIDO, "PEDIDO", "p1", List.of(), null);

        assertEquals("atendente01", r1.getUsuario());
        assertEquals("ATENDENTE", r1.getPerfil());
        assertEquals(Instant.parse("2026-09-23T17:35:00.123Z"), r1.getDataHora());
        assertEquals(1L, r1.getSequencia());
        assertEquals(AuditoriaService.HASH_INICIAL, r1.getHashAnterior());
        assertEquals(r1.getHash(), r2.getHashAnterior());
        assertEquals(2L, r2.getSequencia());
    }

    @Test
    void integridadeDetectaAlteracaoDiretaNoBanco() {
        service.registrar(TipoOperacao.ALTERACAO_CLIENTE, "CLIENTE", "c1",
                List.of(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil")), null);
        service.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, "PEDIDO", "p1", List.of(), "Cliente desistiu");
        assertTrue(service.verificarIntegridade().integro());

        // Simula adulteracao feita diretamente no MongoDB
        banco.get(0).getAlteracoes().get(0).setValorNovo("Rua Falsa");

        ResultadoIntegridade resultado = service.verificarIntegridade();
        assertFalse(resultado.integro());
        assertEquals(Long.valueOf(1L), resultado.primeiraSequenciaInvalida());
    }

    @Test
    void integridadeDetectaRegistroApagado() {
        service.registrar(TipoOperacao.CRIACAO_PEDIDO, "PEDIDO", "p1", List.of(), null);
        service.registrar(TipoOperacao.CRIACAO_PEDIDO, "PEDIDO", "p2", List.of(), null);
        service.registrar(TipoOperacao.CRIACAO_PEDIDO, "PEDIDO", "p3", List.of(), null);
        banco.remove(1);
        assertFalse(service.verificarIntegridade().integro());
    }
}
