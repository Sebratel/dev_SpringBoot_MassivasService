package br.com.sebratel.massivas.dto.massivas.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AberturaRegistroMassivoOutputDTO {
    private boolean success;
    private List<Object> messages;
    private AberturaRegistroMassivoResponseDTO response;
    private String dataResponseType;
    private Double elapsedTime;
}


