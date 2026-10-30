package br.com.sebratel.massivas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MatrixDocumentoInputDTO {

    /** CPF ou CNPJ, com ou sem pontuacao. */
    @NotBlank(message = "O documento e obrigatorio")
    private String documento;
}
