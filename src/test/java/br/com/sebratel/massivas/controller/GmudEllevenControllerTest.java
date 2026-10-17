package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.BaseTest;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoAssignmentDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoResponseDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.service.gmud.AbrirGmudNoEllevenApiService;
import br.com.sebratel.massivas.service.gmud.FinalizarGmudNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.MassivaIdempotencyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GmudEllevenController.class)
@Import(MassivaIdempotencyStore.class)
@AutoConfigureMockMvc(addFilters = false)
class GmudEllevenControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AbrirGmudNoEllevenApiService abrirGmudNoEllevenApiService;

    @MockitoBean
    private FinalizarGmudNoEllevenApiService finalizarGmudNoEllevenApiService;

    private FinalizaRegistroMassivoInputDTO buildCloseInput() {
        return FinalizaRegistroMassivoInputDTO.builder()
                .assignmentId("999")
                .incidentStatusId("4")
                .description("GMUD concluída via plataforma por Fulano em 08/10/2026")
                .progress("0")
                .priority("35")
                .notificationTarget("0")
                .privateReport("true")
                .build();
    }

    private AberturaRegistroMassivoInputDTO buildGmudInput() {
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        input.setIncidentStatusId(1);
        input.setPersonId(30611L);
        input.setIncidentTypeId(1084);
        input.setCatalogServiceId(1082);
        input.setServiceLevelAgreementId(99);
        input.setMatrixType(2);
        input.setTeamCode("2.8");
        input.setSolicitationServiceCategory1("infra - 42");
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("GMUD - Teste");
        assignment.setDescription("Descrição da mudança");
        input.setAssignment(assignment);
        return input;
    }

    @Test
    @DisplayName("Should open GMUD via API")
    void abrirGmudViaApiSuccess() throws Exception {
        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(true);
        AberturaRegistroMassivoResponseDTO responseDTO = new AberturaRegistroMassivoResponseDTO();
        responseDTO.setProtocol(1900000L);
        responseDTO.setAssignmentId(999L);
        output.setResponse(responseDTO);

        when(abrirGmudNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/gmud/abrir-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildGmudInput())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response.protocol").value(1900000L));
    }

    @Test
    @DisplayName("Should return 502 when GMUD open fails on Elleven side")
    void abrirGmudViaApiFail() throws Exception {
        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(false);

        when(abrirGmudNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/gmud/abrir-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildGmudInput())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 500 when GMUD open fails with exception")
    void abrirGmudViaApiException() throws Exception {
        when(abrirGmudNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/v1/gmud/abrir-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildGmudInput())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should finalize GMUD via API")
    void encerrarGmudViaApiSuccess() throws Exception {
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(true)
                .build();

        when(finalizarGmudNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(delete("/api/v1/gmud/encerrar-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCloseInput())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 502 when GMUD finalize fails on Elleven side")
    void encerrarGmudViaApiFail() throws Exception {
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(false)
                .build();

        when(finalizarGmudNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(delete("/api/v1/gmud/encerrar-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCloseInput())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 500 when GMUD finalize throws exception")
    void encerrarGmudViaApiException() throws Exception {
        when(finalizarGmudNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(delete("/api/v1/gmud/encerrar-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCloseInput())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should register a report (relato) on the GMUD protocol without closing it")
    void relatoGmudViaApiSuccess() throws Exception {
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(true)
                .build();
        when(finalizarGmudNoEllevenApiService.executar(any())).thenReturn(output);

        FinalizaRegistroMassivoInputDTO input = buildCloseInput();
        input.setIncidentStatusId("1");
        input.setDescription("Reagendamento da GMUD: janela alterada via plataforma.");

        mockMvc.perform(post("/api/v1/gmud/relato-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should reject a report without the current incidentStatusId")
    void relatoGmudViaApiMissingStatus() throws Exception {
        FinalizaRegistroMassivoInputDTO input = buildCloseInput();
        input.setIncidentStatusId(null);

        mockMvc.perform(post("/api/v1/gmud/relato-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 502 when the report fails on Elleven side")
    void relatoGmudViaApiFail() throws Exception {
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(false)
                .build();
        when(finalizarGmudNoEllevenApiService.executar(any())).thenReturn(output);

        FinalizaRegistroMassivoInputDTO input = buildCloseInput();
        input.setIncidentStatusId("1");

        mockMvc.perform(post("/api/v1/gmud/relato-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadGateway());
    }
}
