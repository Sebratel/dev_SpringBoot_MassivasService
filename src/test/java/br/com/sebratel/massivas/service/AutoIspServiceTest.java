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
        service = new AutoIspService(client, 6);
    }

    private static AutoIspClient.Evento evento(long id, long contrato) {
        return new AutoIspClient.Evento(id, "olt_down", "new", "2026-10-30T10:00:00", null,
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

    private static AutoIspClient.Evento encerrado(long id, long contrato) {
        return new AutoIspClient.Evento(id, "port_down", "closed", "2026-10-30T09:00:00", "2026-10-30T11:00:00",
                List.of(new AutoIspClient.Recurso(contrato, "fulano")));
    }

    @Test
    @DisplayName("Traz os encerrados recentes do cliente, sem repetir os ativos nem os de outros clientes")
    void encerradosRecentes() {
        when(client.configurado()).thenReturn(true);
        when(client.eventosAtivos()).thenReturn(List.of(evento(1, 10)));
        when(client.eventosEncerradosDesde(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(encerrado(1, 10), encerrado(3, 10), encerrado(4, 99)));

        var s = service.situacao(List.of(10L));

        assertEquals(1, s.getEventos().size());
        assertEquals(1, s.getRecentes().size());
        assertEquals(3L, s.getRecentes().get(0).getId());
        assertEquals("2026-10-30T11:00:00", s.getRecentes().get(0).getFim());
        assertEquals(6, s.getRecentesHoras());
    }

    @Test
    @DisplayName("Falha so na lista de encerrados mantem os ativos e deixa os recentes vazios")
    void encerradosFalhamAtivosContinuam() {
        when(client.configurado()).thenReturn(true);
        when(client.eventosAtivos()).thenReturn(List.of(evento(1, 10)));
        when(client.eventosEncerradosDesde(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("fora"));

        var s = service.situacao(List.of(10L));

        assertTrue(s.isDisponivel());
        assertEquals(1, s.getEventos().size());
        assertTrue(s.getRecentes().isEmpty());
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
