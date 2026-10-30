package br.com.sebratel.massivas.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AutoIspServiceTest {

    private AutoIspClient client;
    private AutoIspService service;

    @BeforeEach
    void setUp() {
        client = mock(AutoIspClient.class);
        service = new AutoIspService(client);
    }

    private static AutoIspClient.Evento evento(long id, long contrato) {
        return new AutoIspClient.Evento(id, "olt_down", "new", "2026-10-30T10:00:00",
                List.of(new AutoIspClient.Recurso(contrato, "fulano")));
    }

    @Test
    @DisplayName("Sem configuracao o bloco some (null) e o AutoISP nem e consultado")
    void naoConfigurado() {
        when(client.configurado()).thenReturn(false);

        assertNull(service.situacao(List.of(10L)));
        verify(client).configurado();
        verifyNoMoreInteractions(client);
    }

    @Test
    @DisplayName("Devolve so os eventos que citam contratos do cliente")
    void filtraPorContrato() {
        when(client.configurado()).thenReturn(true);
        when(client.eventosAtivos()).thenReturn(List.of(evento(1, 10), evento(2, 99)));

        var s = service.situacao(List.of(10L, 20L));

        assertTrue(s.isDisponivel());
        assertEquals(1, s.getEventos().size());
        assertEquals(1L, s.getEventos().get(0).getId());
        assertEquals("olt_down", s.getEventos().get(0).getTipo());
        assertEquals(1L, s.getEventos().get(0).getContratosAfetados());
    }

    @Test
    @DisplayName("Sem evento nos contratos: disponivel e lista vazia")
    void semEventos() {
        when(client.configurado()).thenReturn(true);
        when(client.eventosAtivos()).thenReturn(List.of(evento(2, 99)));

        var s = service.situacao(List.of(10L));

        assertTrue(s.isDisponivel());
        assertTrue(s.getEventos().isEmpty());
    }

    @Test
    @DisplayName("AutoISP fora do ar nao derruba: disponivel=false")
    void indisponivel() {
        when(client.configurado()).thenReturn(true);
        when(client.eventosAtivos()).thenThrow(new AutoIspClient.IndisponivelException("fora"));

        var s = service.situacao(List.of(10L));

        assertFalse(s.isDisponivel());
        assertNull(s.getEventos());
    }
}
