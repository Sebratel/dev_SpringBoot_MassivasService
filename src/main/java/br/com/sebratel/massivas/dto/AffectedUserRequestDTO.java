package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AffectedUserRequestDTO {
    private String pppoe;
    @NotNull(message = "O ID do protocolo (protocol) é obrigatório")
    private Long protocol;
    private String reason;
    @NotNull(message = "O ID do contrato (contractId) é obrigatório")
    private Long contractId;
    @NotNull(message = "A data de finalização (finishDate) é obrigatória")
    private LocalDateTime finishDate;
    private LocalDateTime created;
    private String createdBy;
}
