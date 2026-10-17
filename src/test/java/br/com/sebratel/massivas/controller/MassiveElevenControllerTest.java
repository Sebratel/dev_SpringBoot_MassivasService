package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.BaseTest;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaOutputDTO;
import br.com.sebratel.massivas.dto.massivas.EllevenApiResponseDTO;
import br.com.sebratel.massivas.dto.massivas.MassivasBFFOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ResponseDataDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaProtocoloInfraInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoAssignmentDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoResponseDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.service.massivas.AbrirProtocoloInfraNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.AdicionarMassivaNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.AdicionarMassivaNoEllevenService;
import br.com.sebratel.massivas.service.massivas.EnviarListaDeAfetadosParaNativeService;
import br.com.sebratel.massivas.service.massivas.FinalizarMassivaNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.GetAllMassivesService;
import br.com.sebratel.massivas.service.massivas.MassivaIdempotencyStore;
import br.com.sebratel.massivas.service.massivas.RecuperarPrevisaoMassivaPorContratoService;
import br.com.sebratel.massivas.service.massivas.RecuperarTodasAsMassivasPeloBancoService;
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

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MassiveElevenController.class)
@Import(MassivaIdempotencyStore.class)
@AutoConfigureMockMvc(addFilters = false)
class MassiveElevenControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdicionarMassivaNoEllevenService adicionarMassivaNoEllevenService;

    @MockitoBean
    private AdicionarMassivaNoEllevenApiService adicionarMassivaNoEllevenApiService;

    @MockitoBean
    private EnviarListaDeAfetadosParaNativeService enviarListaDeAfetadosParaNativeService;

    @MockitoBean
    private GetAllMassivesService getAllMassivesService;

    @MockitoBean
    private RecuperarTodasAsMassivasPeloBancoService recuperarTodasAsMassivasPeloBancoService;

    @MockitoBean
    private RecuperarPrevisaoMassivaPorContratoService recuperarPrevisaoMassivaPorContratoService;

    @MockitoBean
    private FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService;

    @MockitoBean
    private AbrirProtocoloInfraNoEllevenApiService abrirProtocoloInfraNoEllevenApiService;

    @Test
    @DisplayName("Should create massive incident with flutter data")
    void createMassiveIncidentWithFlutterDataSuccess() throws Exception {
        String json = "{\n" +
                "  \"startDate\": \"06/04/2026\",\n" +
                "  \"startTime\": \"10:00\",\n" +
                "  \"accessPointIds\": [1],\n" +
                "  \"assignmentDescription\": \"Test Description\",\n" +
                "  \"maintenanceDate\": \"06/04/2026\",\n" +
                "  \"maintenanceTime\": \"11:00\",\n" +
                "  \"cookieString\": \"test-cookie\"\n" +
                "}";

        CriacaoDeMassivaOutputDTO output = CriacaoDeMassivaOutputDTO.builder()
                .id("1")
                .build();

        when(adicionarMassivaNoEllevenService.salvarNoBancoERP(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/massive-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should retrieve all massive incidents from Elleven and handle non-null response")
    void retrieveAllMassiveIncidentsNonNullResponse() throws Exception {
        EllevenApiResponseDTO output = new EllevenApiResponseDTO();
        ResponseDataDTO responseData = new ResponseDataDTO();
        responseData.setTotalRecords(10);
        output.setResponse(responseData);

        when(getAllMassivesService.getAllSolicitations()).thenReturn(output);

        mockMvc.perform(get("/api/v1/massive-incidents/getAllMassives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should retrieve all massive incidents from Elleven and handle null response")
    void retrieveAllMassiveIncidentsNullResponse() throws Exception {
        EllevenApiResponseDTO output = new EllevenApiResponseDTO();
        output.setResponse(null);

        when(getAllMassivesService.getAllSolicitations()).thenReturn(output);

        mockMvc.perform(get("/api/v1/massive-incidents/getAllMassives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should retrieve all massive incidents via database")
    void retrieveAllMassiveIncidentsViaDatabaseSuccess() throws Exception {
        List<MassivasBFFOutputDTO> output = new ArrayList<>();

        when(recuperarTodasAsMassivasPeloBancoService.executar()).thenReturn(output);

        mockMvc.perform(get("/api/v1/massive-incidents/recover-via-database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should create massive incident via API")
    void createMassiveIncidentViaApiSuccess() throws Exception {
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        input.setIncidentStatusId(1);
        input.setPersonId(1L);
        input.setIncidentTypeId(1);
        input.setCatalogServiceId(1);
        input.setServiceLevelAgreementId(1);
        input.setMatrixType(1);
        input.setTeamCode("TEAM");
        input.setSolicitationServiceCategory1("CAT1");
        input.setAssignment(new AberturaRegistroMassivoAssignmentDTO());

        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(true);
        AberturaRegistroMassivoResponseDTO responseDTO = new AberturaRegistroMassivoResponseDTO();
        responseDTO.setProtocol(123L);
        output.setResponse(responseDTO);

        when(adicionarMassivaNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/massive-incidents/save-massive-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 502 when create via API fails on Elleven side")
    void createMassiveIncidentViaApiFail() throws Exception {
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        input.setIncidentStatusId(1);
        input.setPersonId(1L);
        input.setIncidentTypeId(1);
        input.setCatalogServiceId(1);
        input.setServiceLevelAgreementId(1);
        input.setMatrixType(1);
        input.setTeamCode("TEAM");
        input.setSolicitationServiceCategory1("CAT1");
        input.setAssignment(new AberturaRegistroMassivoAssignmentDTO());

        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(false);

        when(adicionarMassivaNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/massive-incidents/save-massive-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    private AberturaProtocoloInfraInputDTO buildInfraInput() {
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Protocolo de Infra");
        assignment.setDescription("Mascara de infraestrutura");

        AberturaProtocoloInfraInputDTO input = new AberturaProtocoloInfraInputDTO();
        input.setInfraType("cto_lo");
        input.setPersonId(1L);
        input.setAuthenticationAccessPointCode("CAN-C-5562");
        input.setAssignment(assignment);
        return input;
    }

    @Test
    @DisplayName("Should open infrastructure protocol via API")
    void openInfraSolicitationViaApiSuccess() throws Exception {
        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(true);
        AberturaRegistroMassivoResponseDTO responseDTO = new AberturaRegistroMassivoResponseDTO();
        responseDTO.setProtocol(123L);
        responseDTO.setAssignmentId(456L);
        output.setResponse(responseDTO);

        when(abrirProtocoloInfraNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/massive-incidents/open-infra-solicitation-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInfraInput())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 502 when open infra fails on Elleven side")
    void openInfraSolicitationViaApiFail() throws Exception {
        AberturaRegistroMassivoOutputDTO output = new AberturaRegistroMassivoOutputDTO();
        output.setSuccess(false);

        when(abrirProtocoloInfraNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(post("/api/v1/massive-incidents/open-infra-solicitation-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInfraInput())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 500 when open infra fails with exception")
    void openInfraSolicitationViaApiException() throws Exception {
        when(abrirProtocoloInfraNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/v1/massive-incidents/open-infra-solicitation-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildInfraInput())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should finalize massive incident via API")
    void finalizeMassiveIncidentViaApiSuccess() throws Exception {
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(true)
                .build();

        when(finalizarMassivaNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(delete("/api/v1/massive-incidents/finalize-ticket-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should return 502 when finalization fails on Elleven side")
    void finalizeMassiveIncidentViaApiFail() throws Exception {
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();
        FinalizarRegistroMassivoOutputDTO output = FinalizarRegistroMassivoOutputDTO.builder()
                .success(false)
                .build();

        when(finalizarMassivaNoEllevenApiService.executar(any())).thenReturn(output);

        mockMvc.perform(delete("/api/v1/massive-incidents/finalize-ticket-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should return 500 when creating massive incident fails with exception")
    void createMassiveIncidentWithFlutterDataError() throws Exception {
        String json = "{\n" +
                "  \"startDate\": \"06/04/2026\",\n" +
                "  \"startTime\": \"10:00\",\n" +
                "  \"accessPointIds\": [1],\n" +
                "  \"assignmentDescription\": \"Test Description\",\n" +
                "  \"maintenanceDate\": \"06/04/2026\",\n" +
                "  \"maintenanceTime\": \"11:00\",\n" +
                "  \"cookieString\": \"test-cookie\"\n" +
                "}";

        when(adicionarMassivaNoEllevenService.salvarNoBancoERP(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/v1/massive-incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return 500 when retrieving via database fails")
    void retrieveAllMassiveIncidentsViaDatabaseError() throws Exception {
        when(recuperarTodasAsMassivasPeloBancoService.executar()).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/v1/massive-incidents/recover-via-database"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return 500 when creating via API fails with exception")
    void createMassiveIncidentViaApiException() throws Exception {
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        input.setIncidentStatusId(1);
        input.setPersonId(1L);
        input.setIncidentTypeId(1);
        input.setCatalogServiceId(1);
        input.setServiceLevelAgreementId(1);
        input.setMatrixType(1);
        input.setTeamCode("TEAM");
        input.setSolicitationServiceCategory1("CAT1");
        input.setAssignment(new AberturaRegistroMassivoAssignmentDTO());

        when(adicionarMassivaNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/api/v1/massive-incidents/save-massive-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return 500 when finalization fails with exception")
    void finalizeMassiveIncidentViaApiException() throws Exception {
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();

        when(finalizarMassivaNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(delete("/api/v1/massive-incidents/finalize-ticket-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0].message").value("Internal server error while finalizing massive incident: Error"));
    }

    @Test
    @DisplayName("Should return 502 when finalization fails due to linked protocol error")
    void finalizeMassiveIncidentViaApiLinkedProtocolError() throws Exception {
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();

        when(finalizarMassivaNoEllevenApiService.executar(any())).thenThrow(new RuntimeException("Failed to finalize linked protocol: 123"));

        mockMvc.perform(delete("/api/v1/massive-incidents/finalize-ticket-via-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.messages[0].message").value("Internal server error while finalizing massive incident: Failed to finalize linked protocol: 123"));
    }
}
