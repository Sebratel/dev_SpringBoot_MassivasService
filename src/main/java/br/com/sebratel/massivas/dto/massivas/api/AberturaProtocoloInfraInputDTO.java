package br.com.sebratel.massivas.dto.massivas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Entrada do endpoint de abertura de protocolo de infraestrutura vinculado a uma massiva.
 * O frontend envia o tipo escolhido, o solicitante, o AP principal e o assignment já com a
 * descrição (máscara) pronta. As constantes do ERP (catálogo/tipo/categorias/equipe/SLA/local)
 * são carimbadas no gateway a partir do {@code infraType}.
 */
@Data
public class AberturaProtocoloInfraInputDTO {

    /** Código do tipo de infra: cto_lo | cto_sinal_alto | cto_avariada | backbone. */
    @NotBlank
    private String infraType;

    @NotNull
    private Long personId;

    /** AP principal da massiva (a lista completa vai no descritivo). Opcional. */
    private String authenticationAccessPointCode;

    /** Código do Site — obrigatório para o tipo backbone (matriz interna de Rompimento de Backbone). */
    private String authenticationSiteCode;

    @NotNull
    private AberturaRegistroMassivoAssignmentDTO assignment;
}
