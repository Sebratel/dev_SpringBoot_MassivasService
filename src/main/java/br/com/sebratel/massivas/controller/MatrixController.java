package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.MatrixMassiveInputDTO;
import br.com.sebratel.massivas.dto.MatrixMassiveOutputDTO;
import br.com.sebratel.massivas.service.MatrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matrix")
public class MatrixController {

    final MatrixService matrixService;

    @Autowired
    public MatrixController(MatrixService matrixService) {
        this.matrixService = matrixService;
    }


    @GetMapping
    public ApiResponse<MatrixMassiveOutputDTO> getMassiveInfo(@RequestBody MatrixMassiveInputDTO inputDTO) {
        return ApiResponse.<MatrixMassiveOutputDTO>builder()
                .success(true)
                .message("Massive data retrived sucessfully")
                .data(matrixService.getContractInfoByCPF(inputDTO.getCpf()))
                .build();
    }
}
