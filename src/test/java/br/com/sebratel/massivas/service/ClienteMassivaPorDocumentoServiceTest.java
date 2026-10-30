package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AutoIspSituacaoDTO;
import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.repository.erp.ClientePorDocumentoRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClienteMassivaPorDocumentoServiceTest {

    private ClientePorDocumentoRepository repositorio;
    private AffectedUserService afetados;
    private AutoIspService autoIsp;
    private ClienteMassivaPorDocumentoService servico;

    @BeforeEach
    void setUp() {
        repositorio = mock(ClientePorDocumentoRepository.class);
        afetados = mock(AffectedUserService.class);
        autoIsp = mock(AutoIspService.class);
        var porTelefone = new ClienteMassivaPorTelefoneService(mock(ClientePorTelefoneRepository.class), afetados, 5, autoIsp);
        servico = new ClienteMassivaPorDocumentoService(repositorio, porTelefone);
    }

    @Test
    @DisplayName("Documento sem cliente com contrato: cliente_nao_encontrado, sem consultar massiva")
    void clienteNaoEncontrado() {
        when(repositorio.buscarPorDocumento("12345678901")).thenReturn(List.of());

        var r = servico.consultar("123.456.789-01");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.CLIENTE_NAO_ENCONTRADO, r.getStatus());
        assertEquals("123******01", r.getDocumento());
        assertFalse(r.isEmMassiva());
        verifyNoInteractions(afetados);
    }

    @Test
    @DisplayName("Contrato sem afetados: sem_massiva")
    void semMassiva() {
        when(repositorio.buscarPorDocumento("12345678901")).thenReturn(List.of(new ClienteContrato(1L, 10L)));
        when(afetados.getUsuariosAfetadosByContractId(10L)).thenThrow(new ResourceNotFoundException("nao"));

        var r = servico.consultar("12345678901");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA, r.getStatus());
        assertEquals(1, r.getContratosConsultados());
    }

    @Test
    @DisplayName("Contrato afetado: em_massiva com motivo e previsao")
    void emMassiva() {
        when(repositorio.buscarPorDocumento("12345678901")).thenReturn(List.of(new ClienteContrato(1L, 10L)));
        when(afetados.getUsuariosAfetadosByContractId(10L)).thenReturn(ImpactedUsersOutputDTO.builder()
                .impactedUsers(List.of(Map.of(10L, ImpactDetailsOutputDTO.builder()
                        .reason("Rompimento de fibra")
                        .estimateTimeOfRestoration(3L)
                        .estimatedTimeHour(18L)
                        .build())))
                .build());

        var r = servico.consultar("12345678901");

        assertTrue(r.isEmMassiva());
        assertEquals(ClienteMassivaPorTelefoneOutputDTO.EM_MASSIVA, r.getStatus());
        assertEquals("Rompimento de fibra", r.getMotivo());
        assertEquals(3L, r.getPrevisaoHoras());
    }

    @Test
    @DisplayName("Evento do AutoISP nos contratos acende em_autoisp, mesmo sem massiva")
    void alteracaoDeRedeNoAutoIsp() {
        when(repositorio.buscarPorDocumento("12345678901")).thenReturn(List.of(new ClienteContrato(1L, 10L)));
        when(afetados.getUsuariosAfetadosByContractId(10L)).thenThrow(new ResourceNotFoundException("nao"));
        when(autoIsp.situacao(List.of(10L))).thenReturn(AutoIspSituacaoDTO.builder().disponivel(true)
                .eventos(List.of(AutoIspSituacaoDTO.Evento.builder().id(7L).tipo("olt_down").status("open")
                        .contratosAfetados(1L).build()))
                .build());

        var r = servico.consultar("12345678901");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA, r.getStatus());
        assertFalse(r.isEmMassiva());
        assertTrue(r.isEmAutoisp());
        assertEquals("olt_down", r.getAutoisp().getEventos().get(0).getTipo());
    }

    @Test
    @DisplayName("AutoISP nao configurado: sem bloco autoisp e em_autoisp falso")
    void autoIspNaoConfigurado() {
        when(repositorio.buscarPorDocumento("12345678901")).thenReturn(List.of(new ClienteContrato(1L, 10L)));
        when(afetados.getUsuariosAfetadosByContractId(10L)).thenThrow(new ResourceNotFoundException("nao"));

        var r = servico.consultar("12345678901");

        assertFalse(r.isEmAutoisp());
        assertNull(r.getAutoisp());
    }

    @Test
    @DisplayName("Documento com tamanho invalido e recusado antes de consultar o ERP")
    void documentoInvalido() {
        assertThrows(InvalidMassiveRequestException.class, () -> servico.consultar("123"));
        verifyNoInteractions(repositorio);
    }
}
