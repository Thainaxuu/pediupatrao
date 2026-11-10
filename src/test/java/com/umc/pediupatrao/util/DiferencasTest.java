package com.umc.pediupatrao.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiferencasTest {

    @Test
    void registraSomenteCamposAlterados() {
        Map<String, String> antes = new LinkedHashMap<>();
        antes.put("logradouro", "Rua das Flores");
        antes.put("numero", "10");
        Map<String, String> depois = new LinkedHashMap<>();
        depois.put("logradouro", "Av. Brasil");
        depois.put("numero", "10");

        List<AlteracaoCampo> alteracoes = Diferencas.comparar(antes, depois);

        assertEquals(1, alteracoes.size());
        assertEquals("logradouro", alteracoes.get(0).getCampo());
        assertEquals("Rua das Flores", alteracoes.get(0).getValorAnterior());
        assertEquals("Av. Brasil", alteracoes.get(0).getValorNovo());
    }

    @Test
    void textoVazioEquivaleANulo() {
        assertTrue(Diferencas.comparar(Map.of("complemento", " "), new LinkedHashMap<>()).isEmpty());
    }

    @Test
    void mascaraTelefoneMantendoQuatroUltimosDigitos() {
        assertEquals("*******4321", MascaraDados.telefone("(11) 98765-4321"));
        assertNull(MascaraDados.telefone(null));
    }
}
