package br.com.sebratel.massivas.dto.massivas.api;

import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AberturaRegistroMassivoInputDTO {
    @NotNull
    private Integer incidentStatusId;
    @NotNull
    private Long personId;
    @NotNull
    private Integer incidentTypeId;
    @NotNull
    private Integer catalogServiceId;
    @NotNull
    private Integer serviceLevelAgreementId;
    @NotNull
    private Integer matrixType;
    @NotNull
    private String teamCode;
    @NotBlank
    private String solicitationServiceCategory1;
    private String solicitationServiceCategory2;
    private String solicitationServiceCategory3;
    private String solicitationServiceCategory4;
    private String solicitationServiceCategory5;
    private String authenticationAccessPointCode;
    /**
     * Código do Site (obrigatório para a matriz interna de Backbone no Voalle). NON_NULL para
     * não alterar o payload da massiva/CTO, que não usa esse campo.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String authenticationSiteCode;
    @NotNull
    private AberturaRegistroMassivoAssignmentDTO assignment;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private int affectedUsersQuantity;
    private List<AffectedUsersEntity> affectedUsers = new java.util.ArrayList<>();
}
