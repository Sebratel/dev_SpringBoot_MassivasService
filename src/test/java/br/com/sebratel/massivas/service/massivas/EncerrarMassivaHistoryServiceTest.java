package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivaHistoryEncerramentoInputDTO;
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
class EncerrarMassivaHistoryServiceTest {

    @Mock
    private MassivaHistoryJPARepository massivaHistoryJPARepository;

    @InjectMocks
    private EncerrarMassivaHistoryService service;

    @Test
    void encerrarComInputPersonalizadoUsaValoresInformados() {
        when(massivaHistoryJPARepository.encerrarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(1);

        MassivaHistoryEncerramentoInputDTO input = MassivaHistoryEncerramentoInputDTO.builder()
                .closedBy("joao.silva")
                .closeDescription("Encerrado manualmente")
                .build();

        int atualizados = service.encerrar(123L, input);

        assertEquals(1, atualizados);
        verify(massivaHistoryJPARepository).encerrarPorProtocolo(
                org.mockito.ArgumentMatchers.eq(123L),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq("joao.silva"),
                org.mockito.ArgumentMatchers.eq("Encerrado manualmente"));
    }

    @Test
    void encerrarSemInputUsaValoresPadrao() {
        when(massivaHistoryJPARepository.encerrarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(1);

        int atualizados = service.encerrar(456L, null);

        assertEquals(1, atualizados);
        verify(massivaHistoryJPARepository).encerrarPorProtocolo(
                org.mockito.ArgumentMatchers.eq(456L),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq("reconciliacao-n8n"),
                any(String.class));
    }

    @Test
    void encerrarIdempotenteRetornaZeroQuandoNadaAtualizado() {
        when(massivaHistoryJPARepository.encerrarPorProtocolo(anyLong(), any(LocalDateTime.class), anyString(), anyString()))
                .thenReturn(0);

        int atualizados = service.encerrar(789L, null);

        assertEquals(0, atualizados);
    }
}
