package br.com.sebratel.massivas.dto.massivas;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MassivaHistoryCancelamentoInputDTO {
    private String cancelledBy;
    private String cancelDescription;
}
