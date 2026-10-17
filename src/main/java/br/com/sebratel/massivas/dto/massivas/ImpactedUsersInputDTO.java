package br.com.sebratel.massivas.dto.massivas;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ImpactedUsersInputDTO {
    @NotNull(message = "O campo é obrigatório")
    @NotEmpty(message = "A lista não pode ser vazia")
    @JsonProperty("impactedUsers")
    private List<Map<Long, ImpactDetailsInputDTO>> impactedUsers;
}
