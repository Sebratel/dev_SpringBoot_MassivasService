package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivaHistoryCancelamentoInputDTO;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarMassivaHistoryServiceTest {

    @Mock
    private MassivaHistoryJPARepository massivaHistoryJPARepository;

    @InjectMocks
    private CancelarMassivaHistoryService service;

    @Test
    void cancelarComInputPersonalizadoUsaValoresInformados() {
        when(massivaHistoryJPARepository.cancelarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(1);

        MassivaHistoryCancelamentoInputDTO input = MassivaHistoryCancelamentoInputDTO.builder()
                .cancelledBy("maria.silva")
                .cancelDescription("Cancelado manualmente")
                .build();

        int atualizados = service.cancelar(123L, input);

        assertEquals(1, atualizados);
        verify(massivaHistoryJPARepository).cancelarPorProtocolo(
                org.mockito.ArgumentMatchers.eq(123L),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq("maria.silva"),
                org.mockito.ArgumentMatchers.eq("Cancelado manualmente"));
    }

    @Test
    void cancelarSemInputUsaValoresPadrao() {
        when(massivaHistoryJPARepository.cancelarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(1);

        int atualizados = service.cancelar(456L, null);

        assertEquals(1, atualizados);
        verify(massivaHistoryJPARepository).cancelarPorProtocolo(
                org.mockito.ArgumentMatchers.eq(456L),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq("reconciliacao-n8n"),
                any(String.class));
    }

    @Test
    void cancelarIdempotenteRetornaZeroQuandoNadaAtualizado() {
        when(massivaHistoryJPARepository.cancelarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(0);

        int atualizados = service.cancelar(789L, null);

        assertEquals(0, atualizados);
    }
}
