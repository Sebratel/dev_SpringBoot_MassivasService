package br.com.sebratel.massivas.dto.massivas.api;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FinalizarRegistroMassivoOutputDTO {
    private final boolean success;
    private final List<EllevenCompleteTaskResponseDTO> messages;
    private final Object response;
    private final String dataResponseType;
    private final String elapsedTime;
}
