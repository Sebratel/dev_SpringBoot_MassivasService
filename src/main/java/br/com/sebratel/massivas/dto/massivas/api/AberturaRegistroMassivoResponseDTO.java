package br.com.sebratel.massivas.dto.massivas.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AberturaRegistroMassivoResponseDTO {
    private Long protocol;
    private Long assignmentId;
    private String message;
}
