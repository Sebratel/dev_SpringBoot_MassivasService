package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveInputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveOutputDTO;
import br.com.sebratel.massivas.dto.MatrixTelefoneInputDTO;
import br.com.sebratel.massivas.service.ClienteMassivaPorTelefoneService;
import br.com.sebratel.massivas.service.MatrixService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matrix")
public class MatrixController {

    final MatrixService matrixService;
    final ClienteMassivaPorTelefoneService clienteMassivaPorTelefoneService;

    @Autowired
    public MatrixController(MatrixService matrixService,
                            ClienteMassivaPorTelefoneService clienteMassivaPorTelefoneService) {
        this.matrixService = matrixService;
        this.clienteMassivaPorTelefoneService = clienteMassivaPorTelefoneService;
    }


    @GetMapping
    public ApiResponse<MatrixMassiveOutputDTO> getMassiveInfo(@RequestBody MatrixMassiveInputDTO inputDTO) {
        return ApiResponse.<MatrixMassiveOutputDTO>builder()
                .success(true)
                .message("Massive data retrived sucessfully")
                .data(matrixService.getContractInfoByCPF(inputDTO.getCpf()))
                .build();
    }

    /**
     * Situacao de massiva de quem ligou, a partir do telefone (alerta ao atendente). Exige autenticacao no
     * gateway, ao contrario do GET por CPF: devolve se um telefone pertence a um cliente e o motivo da massiva.
     */
    @PostMapping("/telefone")
    public ApiResponse<ClienteMassivaPorTelefoneOutputDTO> getMassiveInfoByPhone(
            @Valid @RequestBody MatrixTelefoneInputDTO input) {
        return ApiResponse.<ClienteMassivaPorTelefoneOutputDTO>builder()
                .success(true)
                .message("Massive data by phone retrieved successfully")
                .data(clienteMassivaPorTelefoneService.consultar(input.getTelefone()))
                .build();
    }
}
