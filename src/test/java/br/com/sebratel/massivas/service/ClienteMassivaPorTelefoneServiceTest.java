package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteMassivaPorTelefoneServiceTest {

    @Mock
    private ClientePorTelefoneRepository repository;

    @Mock
    private AffectedUserService affectedUserService;

    private ClienteMassivaPorTelefoneService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new ClienteMassivaPorTelefoneService(repository, affectedUserService, 5, mock(AutoIspService.class),
                mock(HistoricoMassivaService.class));
    }

    @Test
    void telefoneEmMuitosClientesNaoIdentificaQuemLigou() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of(
                new ClienteContrato(1L, 10L), new ClienteContrato(2L, 20L), new ClienteContrato(3L, 30L),
                new ClienteContrato(4L, 40L), new ClienteContrato(5L, 50L), new ClienteContrato(6L, 60L)));

        var saida = service.consultar("51995664811");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.TELEFONE_COMPARTILHADO, saida.getStatus());
        assertFalse(saida.isEmMassiva());
        assertEquals(6, saida.getClientesEncontrados());
        verifyNoInteractions(affectedUserService);
    }

    private ImpactedUsersOutputDTO impacto(Long contrato, String motivo, long horas, long hora) {
        return ImpactedUsersOutputDTO.builder()
                .impactedUsers(List.of(Map.of(contrato, ImpactDetailsOutputDTO.builder()
                        .reason(motivo)
                        .estimateTimeOfRestoration(horas)
                        .estimatedTimeHour(hora)
                        .build())))
                .build();
    }

    @Test
    void telefoneSemClienteNoErpRetornaClienteNaoEncontrado() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of());

        var saida = service.consultar("51995664811");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.CLIENTE_NAO_ENCONTRADO, saida.getStatus());
        assertFalse(saida.isEmMassiva());
        assertEquals("51*******11", saida.getTelefone());
        verifyNoInteractions(affectedUserService);
    }

    @Test
    void clienteSemContratoEmMassivaRetornaSemMassiva() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of(new ClienteContrato(1L, 100L)));
        when(affectedUserService.getUsuariosAfetadosByContractId(100L))
                .thenThrow(new ResourceNotFoundException("Usuário afetado não encontrado"));

        var saida = service.consultar("51995664811");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA, saida.getStatus());
        assertFalse(saida.isEmMassiva());
        assertEquals(1, saida.getClientesEncontrados());
        assertEquals(1, saida.getContratosConsultados());
        assertNull(saida.getContratosEmMassiva());
    }

    @Test
    void previsaoVemComDataExataEIndicaQuandoJaVenceu() {
        var amanha = java.time.LocalDateTime.now().plusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
        var ontem = java.time.LocalDateTime.now().minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
        when(repository.buscarPorVariantes(anyCollection()))
                .thenReturn(List.of(new ClienteContrato(1L, 100L), new ClienteContrato(1L, 200L)));
        when(affectedUserService.getUsuariosAfetadosByContractId(100L)).thenReturn(ImpactedUsersOutputDTO.builder()
                .impactedUsers(List.of(Map.of(100L, ImpactDetailsOutputDTO.builder().reason("a")
                        .estimateTimeOfRestoration(30L).estimatedTimeHour(23L).estimatedFinish(amanha).build())))
                .build());
        when(affectedUserService.getUsuariosAfetadosByContractId(200L)).thenReturn(ImpactedUsersOutputDTO.builder()
                .impactedUsers(List.of(Map.of(200L, ImpactDetailsOutputDTO.builder().reason("b")
                        .estimateTimeOfRestoration(1L).estimatedTimeHour(23L).estimatedFinish(ontem).build())))
                .build());

        var saida = service.consultar("51995664811");

        assertEquals(ontem.toString(), saida.getPrevisaoEm());
        assertTrue(saida.getPrevisaoVencida());
        assertEquals(amanha.toString(), saida.getContratosEmMassiva().get(1).getPrevisaoEm());
        assertFalse(saida.getContratosEmMassiva().get(1).getPrevisaoVencida());
    }

    @Test
    void semMassivaAtivaTrazAsMassivasRecentesDoCliente() {
        var historico = mock(HistoricoMassivaService.class);
        var passada = br.com.sebratel.massivas.dto.HistoricoMassivaDTO.builder().protocolo(77L).status("encerrada")
                .build();
        when(historico.recentes(List.of(100L))).thenReturn(List.of(passada));
        when(historico.horas()).thenReturn(6);
        var servico = new ClienteMassivaPorTelefoneService(repository, affectedUserService, 5,
                mock(AutoIspService.class), historico);
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of(new ClienteContrato(1L, 100L)));
        when(affectedUserService.getUsuariosAfetadosByContractId(100L))
                .thenThrow(new ResourceNotFoundException("Usuário afetado não encontrado"));

        var saida = servico.consultar("51995664811");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA, saida.getStatus());
        assertEquals(1, saida.getHistorico().size());
        assertEquals(77L, saida.getHistorico().get(0).getProtocolo());
        assertEquals(6, saida.getHistoricoHoras());
    }

    @Test
    void contratoEmMassivaRetornaMotivoEPrevisao() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of(new ClienteContrato(1L, 100L)));
        when(affectedUserService.getUsuariosAfetadosByContractId(100L)).thenReturn(impacto(100L, "Rompimento de fibra", 3, 18));

        var saida = service.consultar("(51) 99566-4811");

        assertEquals(ClienteMassivaPorTelefoneOutputDTO.EM_MASSIVA, saida.getStatus());
        assertTrue(saida.isEmMassiva());
        assertEquals("Rompimento de fibra", saida.getMotivo());
        assertEquals(3L, saida.getPrevisaoHoras());
        assertEquals(18L, saida.getPrevisaoHora());
        assertEquals(1, saida.getContratosEmMassiva().size());
        assertEquals(100L, saida.getContratosEmMassiva().get(0).getContratoId());
    }

    @Test
    void variosContratosVarreTodosEUsaAMenorPrevisao() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of(
                new ClienteContrato(1L, 100L), new ClienteContrato(1L, 101L), new ClienteContrato(2L, 200L)));
        when(affectedUserService.getUsuariosAfetadosByContractId(100L))
                .thenThrow(new ResourceNotFoundException("não encontrado"));
        when(affectedUserService.getUsuariosAfetadosByContractId(101L)).thenReturn(impacto(101L, "Queda de energia", 5, 20));
        when(affectedUserService.getUsuariosAfetadosByContractId(200L)).thenReturn(impacto(200L, "Rompimento de fibra", 2, 17));

        var saida = service.consultar("51995664811");

        assertEquals(2, saida.getClientesEncontrados());
        assertEquals(3, saida.getContratosConsultados());
        assertEquals(2, saida.getContratosEmMassiva().size());
        assertEquals(2L, saida.getPrevisaoHoras());
        assertEquals("Rompimento de fibra", saida.getMotivo());
    }

    @Test
    void consultaOErpComAsVariantesComENoveDigito() {
        when(repository.buscarPorVariantes(anyCollection())).thenReturn(List.of());

        service.consultar("+55 51 99566-4811");

        verify(repository).buscarPorVariantes(Set.of("51995664811", "5195664811"));
    }

    @Test
    void telefoneInvalidoNaoConsultaOErp() {
        assertThrows(InvalidMassiveRequestException.class, () -> service.consultar("1234"));
        verifyNoInteractions(repository, affectedUserService);
    }
}
