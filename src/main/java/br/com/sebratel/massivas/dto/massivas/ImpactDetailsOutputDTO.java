package br.com.sebratel.massivas.dto.massivas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImpactDetailsOutputDTO {
    @NotBlank(message = "A causa não pode estar em branco")
    String reason;
    @NotNull(message = "A data de previsão não pode ser nula")
    Long estimateTimeOfRestoration;
    @NotNull(message = "Hora prevista")
    Long estimatedTimeHour;
    /** Data e hora exatas da previsao; so uso interno (nao sai nas respostas dos outros endpoints). */
    @JsonIgnore
    LocalDateTime estimatedFinish;
}
