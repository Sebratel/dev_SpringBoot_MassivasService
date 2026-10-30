package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.HistoricoMassivaDTO;
import br.com.sebratel.massivas.repository.afetados.impl.HistoricoClienteProjection;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoricoMassivaServiceTest {

    private MassivaHistoryJPARepository repository;
    private HistoricoMassivaService service;

    @BeforeEach
    void setUp() {
        repository = mock(MassivaHistoryJPARepository.class);
        service = new HistoricoMassivaService(repository, 6);
    }

    private static HistoricoClienteProjection linha(long protocolo, String status, LocalDateTime aberta,
                                                    LocalDateTime encerrada, String descricao) {
        HistoricoClienteProjection l = mock(HistoricoClienteProjection.class);
        when(l.getProtocolo()).thenReturn(protocolo);
        when(l.getTitulo()).thenReturn("OLT 1");
        when(l.getStatus()).thenReturn(status);
        when(l.getAbertaEm()).thenReturn(Timestamp.valueOf(aberta));
        when(l.getEncerradaEm()).thenReturn(encerrada == null ? null : Timestamp.valueOf(encerrada));
        when(l.getDescricao()).thenReturn(descricao);
        when(l.getOrigem()).thenReturn("INT6");
        when(l.getContratos()).thenReturn(2L);
        return l;
    }

    @Test
    @DisplayName("Converte as massivas recentes do cliente, com abertura e encerramento")
    void convertePassadas() {
        var aberta = LocalDateTime.of(2026, 10, 30, 9, 0);
        var encerrada = LocalDateTime.of(2026, 10, 30, 11, 30);
        var l = linha(77L, "encerrada", aberta, encerrada, "Normalizada");
        when(repository.historicoDosContratos(any(), any())).thenReturn(List.of(l));

        List<HistoricoMassivaDTO> r = service.recentes(List.of(91836L));

        assertEquals(1, r.size());
        assertEquals(77L, r.get(0).getProtocolo());
        assertEquals("encerrada", r.get(0).getStatus());
        assertEquals("2026-10-30T09:00", r.get(0).getAbertaEm());
        assertEquals("2026-10-30T11:30", r.get(0).getEncerradaEm());
        assertEquals("Normalizada", r.get(0).getDescricaoEncerramento());
        assertEquals(2L, r.get(0).getContratosAfetados());
    }

    @Test
    @DisplayName("Massiva ainda aberta vem sem data de encerramento")
    void abertaSemEncerramento() {
        var l = linha(78L, "aberta", LocalDateTime.of(2026, 10, 30, 13, 0), null, null);
        when(repository.historicoDosContratos(any(), any())).thenReturn(List.of(l));

        assertNull(service.recentes(List.of(1L)).get(0).getEncerradaEm());
    }

    @Test
    @DisplayName("A janela consultada e de 6 horas para tras")
    void janelaDeSeisHoras() {
        when(repository.historicoDosContratos(any(), any())).thenReturn(List.of());
        var antes = LocalDateTime.now().minusHours(6).minusMinutes(1);

        service.recentes(List.of(1L));

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).historicoDosContratos(any(Collection.class), desde.capture());
        // O relogio usa America/Sao_Paulo; aceita diferenca de fuso do servidor de teste, mas nao de horas de janela.
        assertTrue(desde.getValue().isAfter(antes.minusHours(24)) && desde.getValue().isBefore(antes.plusHours(24)));
        assertEquals(6, service.horas());
    }

    @Test
    @DisplayName("Descricao longa e cortada")
    void cortaDescricao() {
        var l = linha(79L, "encerrada", LocalDateTime.now(), LocalDateTime.now(), "x".repeat(500));
        when(repository.historicoDosContratos(any(), any())).thenReturn(List.of(l));

        assertEquals(300, service.recentes(List.of(1L)).get(0).getDescricaoEncerramento().length());
    }

    @Test
    @DisplayName("Sem contratos nao consulta o banco")
    void semContratos() {
        assertTrue(service.recentes(List.of()).isEmpty());
        verify(repository, never()).historicoDosContratos(any(), any());
    }

    @Test
    @DisplayName("Falha do banco devolve lista vazia, sem derrubar a consulta")
    void falhaDoBanco() {
        when(repository.historicoDosContratos(any(), any())).thenThrow(new IllegalStateException("db fora"));

        assertTrue(service.recentes(List.of(1L)).isEmpty());
    }
}
