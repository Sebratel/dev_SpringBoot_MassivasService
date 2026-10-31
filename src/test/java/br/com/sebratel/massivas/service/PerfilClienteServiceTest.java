package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import br.com.sebratel.massivas.repository.afetados.impl.TituloFrequenteProjection;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import br.com.sebratel.massivas.utils.TelefoneBrasil;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerfilClienteServiceTest {

    private final ClientePorTelefoneRepository clientes = mock(ClientePorTelefoneRepository.class);
    private final MassivaHistoryJPARepository historico = mock(MassivaHistoryJPARepository.class);
    private final PerfilClienteService servico = new PerfilClienteService(clientes, historico);

    @Test
    void telefonesNoMesmoFormatoSemRepeticaoEIgnorandoRamal() {
        when(clientes.telefonesDosClientes(anyCollection()))
                .thenReturn(List.of("+55 (51) 99677-2147", "051996772147", "51996772147", "5107", "anonymous", "(51) 3523-3096"));
        var perfil = servico.perfil(List.of(new ClienteContrato(1L, 100L), new ClienteContrato(1L, 101L)));
        assertEquals(1, perfil.getRegistrosPeople());
        assertEquals(2, perfil.getContratos());
        assertEquals(List.of("5135233096", "51996772147"), perfil.getTelefones());
    }

    @Test
    void contaMassivasPorJanela() {
        when(clientes.telefonesDosClientes(anyCollection())).thenReturn(List.of());
        when(historico.contarDosContratos(anyCollection(), any(LocalDateTime.class))).thenReturn(1L, 3L, 7L);
        when(historico.ultimaAberturaDosContratos(anyCollection(), any(LocalDateTime.class)))
                .thenReturn(Timestamp.valueOf("2026-10-20 10:00:00"));
        var titulo = mock(TituloFrequenteProjection.class);
        when(titulo.getTitulo()).thenReturn("OLT Parobe");
        when(titulo.getVezes()).thenReturn(4L);
        when(historico.titulosFrequentesDosContratos(anyCollection(), any(LocalDateTime.class))).thenReturn(List.of(titulo));
        var m = servico.perfil(List.of(new ClienteContrato(1L, 100L))).getMassivas();
        assertEquals(1, m.getUltimos30Dias());
        assertEquals(3, m.getUltimos90Dias());
        assertEquals(7, m.getUltimos12Meses());
        assertEquals("2026-10-20T10:00", m.getUltimaEm());
        assertEquals("OLT Parobe", m.getAssuntos().get(0).getTitulo());
    }

    @Test
    void semClienteDevolvePerfilVazioSemConsultarBanco() {
        var perfil = servico.perfil(List.of());
        assertEquals(0, perfil.getRegistrosPeople());
        assertTrue(perfil.getTelefones().isEmpty());
        assertEquals(null, perfil.getMassivas());
    }

    @Test
    void falhaNoBancoNaoDerrubaOPerfil() {
        when(clientes.telefonesDosClientes(anyCollection())).thenThrow(new IllegalStateException("erp fora"));
        when(historico.contarDosContratos(anyCollection(), any(LocalDateTime.class))).thenThrow(new IllegalStateException("db fora"));
        var perfil = servico.perfil(List.of(new ClienteContrato(1L, 100L)));
        assertEquals(1, perfil.getRegistrosPeople());
        assertTrue(perfil.getTelefones().isEmpty());
        assertEquals(null, perfil.getMassivas());
    }

    @Test
    void canonicoRejeitaQuemNaoTemDdd() {
        assertEquals(Optional.empty(), TelefoneBrasil.canonico("0996772147"));
        assertEquals(Optional.empty(), TelefoneBrasil.canonico("996772147"));
        assertEquals(Optional.empty(), TelefoneBrasil.canonico("anonymous"));
        assertEquals(Optional.of("51996772147"), TelefoneBrasil.canonico("+5551996772147"));
    }
}
