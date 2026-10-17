package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.BaseTest;
import br.com.sebratel.massivas.dto.MatrixMassiveInputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveOutputDTO;
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
