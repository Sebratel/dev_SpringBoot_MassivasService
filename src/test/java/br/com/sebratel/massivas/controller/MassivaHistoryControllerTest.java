package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.BaseTest;
import br.com.sebratel.massivas.dto.massivas.MassivaHistoryOutputDTO;
import br.com.sebratel.massivas.service.massivas.CancelarMassivaHistoryService;
import br.com.sebratel.massivas.service.massivas.EncerrarMassivaHistoryService;
import br.com.sebratel.massivas.service.massivas.RecuperarTodasAsMassivasHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MassivaHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class MassivaHistoryControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecuperarTodasAsMassivasHistoryService recuperarTodasAsMassivasHistoryService;

    @MockitoBean
    private EncerrarMassivaHistoryService encerrarMassivaHistoryService;

    @MockitoBean
    private CancelarMassivaHistoryService cancelarMassivaHistoryService;

    @Test
    void retrieveAllMassivaHistoryViaDatabaseRetornaOk() throws Exception {
        when(recuperarTodasAsMassivasHistoryService.executar()).thenReturn(List.<MassivaHistoryOutputDTO>of());

        mockMvc.perform(get("/api/v1/massivas-history/recuperar-pelo-banco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void finalizeMassivaHistoryRetornaOkQuandoAtualizaLinha() throws Exception {
        when(encerrarMassivaHistoryService.encerrar(eq(1833807L), any())).thenReturn(1);

        mockMvc.perform(patch("/api/v1/massivas-history/1833807/encerrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.atualizados").value(1));
    }

    @Test
    void cancelMassivaHistoryRetornaOkQuandoAtualizaLinha() throws Exception {
        when(cancelarMassivaHistoryService.cancelar(eq(1833807L), any())).thenReturn(1);

        mockMvc.perform(patch("/api/v1/massivas-history/1833807/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.atualizados").value(1));
    }

    @Test
    void cancelMassivaHistoryIdempotenteRetornaMensagemDeNadaAtualizado() throws Exception {
        when(cancelarMassivaHistoryService.cancelar(eq(999L), any())).thenReturn(0);

        mockMvc.perform(patch("/api/v1/massivas-history/999/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.atualizados").value(0));
    }
}
