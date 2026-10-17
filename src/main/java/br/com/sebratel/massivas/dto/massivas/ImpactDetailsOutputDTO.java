package br.com.sebratel.massivas.dto.massivas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImpactDetailsOutputDTO {
    @NotBlank(message = "A causa não pode estar em branco")
    String reason;
    @NotNull(message = "A data de previsão não pode ser nula")
    Long estimateTimeOfRestoration;
    @NotNull(message = "Hora prevista")
    Long estimatedTimeHour;
}
