package br.com.sebratel.massivas.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Telefone de quem ligou, no corpo e nao na URL para nao cair em log de acesso. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixTelefoneInputDTO {

    @NotBlank(message = "O telefone e obrigatorio")
    private String telefone;
}
