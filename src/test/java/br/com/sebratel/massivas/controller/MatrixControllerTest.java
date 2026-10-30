package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.BaseTest;
import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveInputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveOutputDTO;
import br.com.sebratel.massivas.service.ClienteMassivaPorTelefoneService;
import br.com.sebratel.massivas.service.MatrixService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatrixController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatrixControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatrixService matrixService;

    @MockitoBean
    private ClienteMassivaPorTelefoneService clienteMassivaPorTelefoneService;

    @Test
    @DisplayName("POST /telefone devolve a situacao de massiva de quem ligou")
    void getMassiveInfoByPhone_Success() throws Exception {
        when(clienteMassivaPorTelefoneService.consultar("51995664811")).thenReturn(
                ClienteMassivaPorTelefoneOutputDTO.builder()
                        .status(ClienteMassivaPorTelefoneOutputDTO.EM_MASSIVA)
                        .emMassiva(true)
                        .telefone("51*******11")
                        .motivo("Rompimento de fibra")
                        .previsaoHoras(2L)
                        .build());

        mockMvc.perform(post("/api/v1/matrix/telefone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"51995664811\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("em_massiva"))
                .andExpect(jsonPath("$.data.em_massiva").value(true))
                .andExpect(jsonPath("$.data.motivo").value("Rompimento de fibra"))
                .andExpect(jsonPath("$.data.previsao_horas").value(2));
    }

    @Test
    @DisplayName("POST /telefone sem telefone responde 400")
    void getMassiveInfoByPhone_BlankPhone() throws Exception {
        mockMvc.perform(post("/api/v1/matrix/telefone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return massive info when CPF is provided")
    void getMassiveInfoSuccess() throws Exception {
        MatrixMassiveInputDTO input = new MatrixMassiveInputDTO();
        input.setCpf("12345678901");
        MatrixMassiveOutputDTO output = MatrixMassiveOutputDTO.builder().build();

        when(matrixService.getContractInfoByCPF(anyString())).thenReturn(output);

        mockMvc.perform(get("/api/v1/matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Massive data retrived sucessfully"));
    }
}
