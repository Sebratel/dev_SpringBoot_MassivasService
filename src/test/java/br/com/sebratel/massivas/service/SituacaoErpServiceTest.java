package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.ContratosSituacaoDTO;
import br.com.sebratel.massivas.repository.erp.ProtocoloErpRepository;
import br.com.sebratel.massivas.repository.erp.SituacaoContratoRepository;
import br.com.sebratel.massivas.repository.erp.SituacaoContratoRepository.Linha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SituacaoErpServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 10, 0);

    private SituacaoContratoRepository contratos;
    private ProtocoloErpRepository protocolos;
    private SituacaoErpService service;

    @BeforeEach
    void setUp() {
        contratos = mock(SituacaoContratoRepository.class);
        protocolos = mock(ProtocoloErpRepository.class);
        service = new SituacaoErpService(contratos, protocolos, 12, 180);
    }

    private static Linha linha(long id, String status, LocalDate cancelamento, LocalDateTime inicio, String motivo) {
        return new Linha(id, status, cancelamento, inicio, motivo);
    }

    @Test
    @DisplayName("Bloqueio financeiro: dias contados desde o inicio do estado atual, nao de number_of_days")
    void bloqueioFinanceiro() {
        var r = service.montarContratos(3, List.of(
                linha(1, "Bloqueio Financeiro", null, LocalDateTime.of(2026, 10, 20, 8, 0), null)), AGORA);

        assertTrue(r.isDisponivel());
        assertEquals(3, r.getTotalContratos());
        assertEquals(1, r.getProblemas().size());
        var p = r.getProblemas().get(0);
        assertEquals(ContratosSituacaoDTO.BLOQUEIO_FINANCEIRO, p.getSituacao());
        assertEquals(13L, p.getDias());
        assertEquals("2026-10-20", p.getDesde());
        assertEquals(0, r.getOutros());
    }

    @Test
    @DisplayName("Suspenso e bloqueio administrativo tambem aparecem, com o tempo")
    void suspensoEAdministrativo() {
        var r = service.montarContratos(2, List.of(
                linha(1, "Suspenso", null, LocalDateTime.of(2026, 11, 1, 8, 0), null),
                linha(2, "Bloqueio Administrativo", null, LocalDateTime.of(2026, 10, 2, 8, 0), null)), AGORA);

        assertEquals(List.of(ContratosSituacaoDTO.BLOQUEIO_ADMINISTRATIVO, ContratosSituacaoDTO.SUSPENSO),
                r.getProblemas().stream().map(ContratosSituacaoDTO.Problema::getSituacao).toList());
        assertEquals(31L, r.getProblemas().get(0).getDias());
    }

    @Test
    @DisplayName("Cancelado recente traz a data e o motivo sem o prefixo 'Cancelamento -'")
    void canceladoComMotivo() {
        var r = service.montarContratos(1, List.of(linha(9, "Cancelado", LocalDate.of(2026, 9, 2), null,
                "Cancelamento - Cliente Não Instalado - Desistência")), AGORA);

        var p = r.getProblemas().get(0);
        assertEquals(ContratosSituacaoDTO.CANCELADO, p.getSituacao());
        assertEquals("2026-09-02", p.getDesde());
        assertEquals(61L, p.getDias());
        assertEquals("Cliente Não Instalado - Desistência", p.getMotivo());
    }

    @Test
    @DisplayName("Cancelamento com mais de 12 meses nao aparece (so ruido); sem problema a lista vai vazia")
    void canceladoAntigoNaoAparece() {
        var r = service.montarContratos(4, List.of(
                linha(9, "Cancelado", LocalDate.of(2025, 10, 1), null, "Cancelamento - Mudança de Endereço")), AGORA);

        assertTrue(r.isDisponivel());
        assertTrue(r.getProblemas().isEmpty());
    }

    @Test
    @DisplayName("Bloqueios antes dos cancelados, no maximo 3 e o resto vira 'outros'")
    void ordemELimite() {
        var r = service.montarContratos(6, List.of(
                linha(1, "Cancelado", LocalDate.of(2026, 10, 30), null, null),
                linha(2, "Cancelado", LocalDate.of(2026, 10, 1), null, null),
                linha(3, "Bloqueio Financeiro", null, LocalDateTime.of(2026, 10, 25, 0, 0), null),
                linha(4, "Suspenso", null, LocalDateTime.of(2026, 10, 10, 0, 0), null)), AGORA);

        assertEquals(List.of(4L, 3L, 1L), r.getProblemas().stream()
                .map(ContratosSituacaoDTO.Problema::getContratoId).toList());
        assertEquals(1, r.getOutros());
    }

    @Test
    @DisplayName("Motivo de cancelamento sem prefixo ou vazio")
    void motivo() {
        assertEquals("Inadimplência", SituacaoErpService.motivoCancelamento("Cancelamento - Inadimplência"));
        assertEquals("Outro", SituacaoErpService.motivoCancelamento("Outro"));
        assertNull(SituacaoErpService.motivoCancelamento(" "));
        assertNull(SituacaoErpService.motivoCancelamento(null));
    }

    @Test
    @DisplayName("ERP fora do ar: o bloco sai disponivel=false e nao lanca excecao")
    void erpIndisponivel() {
        when(contratos.problemasDosClientes(anyCollection())).thenThrow(new IllegalStateException("fora"));
        when(protocolos.abertosDosClientes(anyCollection())).thenThrow(new IllegalStateException("fora"));

        assertFalse(service.situacaoContratos(List.of(1L)).isDisponivel());
        assertFalse(service.protocolosAbertos(List.of(1L)).isDisponivel());
    }

    @Test
    @DisplayName("Sem clientes o ERP nem e consultado")
    void semClientes() {
        assertNull(service.situacaoContratos(List.of()));
        assertNull(service.protocolosAbertos(List.of()));
        verifyNoInteractions(contratos, protocolos);
    }

    @Test
    @DisplayName("Protocolos: ha_dias, antigo (corte 180 dias), tecnico, visita agendada e limite de 5")
    void protocolos() {
        var linhas = List.of(
                new ProtocoloErpRepository.Linha(100, 1, "TEC-MANUT-ACESSO-CLI-INCIDENTE/PROBLEMA", "Agendado",
                        LocalDateTime.of(2026, 10, 30, 9, 0)),
                new ProtocoloErpRepository.Linha(101, 2, "FIN - Alteração de Vencimento", "Abertura",
                        LocalDateTime.of(2026, 1, 2, 9, 0)),
                new ProtocoloErpRepository.Linha(102, 3, "GERAL - Registro de Atendimento Inicial", "Visita 1",
                        LocalDateTime.of(2026, 10, 1, 9, 0)),
                new ProtocoloErpRepository.Linha(103, 4, "x", "Andamento", LocalDateTime.of(2026, 9, 1, 9, 0)),
                new ProtocoloErpRepository.Linha(104, 5, "x", "Andamento", LocalDateTime.of(2026, 8, 1, 9, 0)),
                new ProtocoloErpRepository.Linha(105, 6, "x", "Andamento", LocalDateTime.of(2020, 1, 1, 9, 0)));

        var r = service.montarProtocolos(linhas, Map.of(1L, LocalDateTime.of(2026, 11, 3, 14, 0)), AGORA);

        assertTrue(r.isDisponivel());
        assertEquals(6, r.getTotal());
        assertEquals(5, r.getItens().size());
        var primeiro = r.getItens().get(0);
        assertEquals(3L, primeiro.getHaDias());
        assertTrue(primeiro.isTecnico());
        assertFalse(primeiro.isAntigo());
        assertEquals("2026-11-03T14:00", primeiro.getVisitaEm());
        var financeiro = r.getItens().get(1);
        assertFalse(financeiro.isTecnico());
        assertTrue(financeiro.isAntigo());
        assertNull(financeiro.getVisitaEm());
        // situacao "Visita 1" torna tecnico mesmo com tipo generico
        assertTrue(r.getItens().get(2).isTecnico());
    }

    @Test
    @DisplayName("Sem protocolo aberto: disponivel, total 0 e lista vazia")
    void semProtocolos() {
        var r = service.montarProtocolos(List.of(), Map.of(), AGORA);

        assertTrue(r.isDisponivel());
        assertEquals(0, r.getTotal());
        assertTrue(r.getItens().isEmpty());
    }
}
