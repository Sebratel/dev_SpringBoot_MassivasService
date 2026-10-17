package br.com.sebratel.massivas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateImpactedUsersInputDTO {
    @NotEmpty(message = "A lista de usuários afetados não pode estar vazia")
    @Valid
    List<AffectedUserRequestDTO> usuarioAfetadoEntities;
    @NotNull(message = "O ID do assignment (assignmentId) é obrigatório")
    Long assignmentId;
}
