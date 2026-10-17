package br.com.sebratel.massivas.dto.massivas;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriacaoDeMassivaInputDTO {

    @NotNull(message = "A data de início (startDate) é obrigatória")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate startDate;

    @NotNull(message = "O horário de início (startTime) é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotEmpty(message = "É necessário informar ao menos um ID de ponto de acesso")
    private Integer[] accessPointIds;

    private Integer[] slotOlt;
    private Integer[] portaOlt;
    private Integer[] addressListId;

    @Builder.Default
    private Integer companyPlaceId = 1;

    private Integer assignmentTypeId;

    @NotBlank(message = "A descrição da designação não pode estar em branco")
    private String assignmentDescription;

    @NotNull(message = "A data de manutenção é obrigatória")
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate maintenanceDate;

    @NotNull(message = "O horário de manutenção é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime maintenanceTime;

    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cookieString;
}