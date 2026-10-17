package br.com.sebratel.massivas.dto.massivas.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EllevenCompleteTaskResponseDTO {
    String message;
    String type;
    String code;
}
