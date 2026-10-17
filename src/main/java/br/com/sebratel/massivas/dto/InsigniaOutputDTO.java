package br.com.sebratel.massivas.dto;

import br.com.sebratel.massivas.repository.erp.projections.InsigniaProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InsigniaOutputDTO {
    Long id;
    String code;
    String title;

    public static InsigniaOutputDTO fromProjection(InsigniaProjection insigniaProjection) {
        return new InsigniaOutputDTO(
                insigniaProjection.getId(),
                insigniaProjection.getCode(),
                insigniaProjection.getTitle()
        );
    }
}
