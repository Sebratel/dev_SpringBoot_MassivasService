package br.com.sebratel.massivas.dto;

import br.com.sebratel.massivas.model.entity.AuthenticationSiteEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AuthenticationSitesOutputDTO {
    Long id;
    /** Título do site — é também o "Código" para sites de POP/DC (vai em authenticationSiteCode). */
    String title;
    String city;
    String neighborhood;

    public static AuthenticationSitesOutputDTO fromEntity(AuthenticationSiteEntity authenticationSiteEntity) {
        return new AuthenticationSitesOutputDTO(
                authenticationSiteEntity.getId(),
                authenticationSiteEntity.getTitle(),
                authenticationSiteEntity.getCity(),
                authenticationSiteEntity.getNeighborhood()
        );
    }
}
