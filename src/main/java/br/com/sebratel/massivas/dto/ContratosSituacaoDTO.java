package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Contratos do cliente que o atendente precisa saber que estao fora do normal: bloqueados, suspensos ou cancelados
 * (de forma recente). Contrato ativo nao entra; sem problema a lista vai vazia e a extensao nao mostra o bloco.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContratosSituacaoDTO {

    public static final String BLOQUEIO_FINANCEIRO = "bloqueio_financeiro";
    public static final String BLOQUEIO_ADMINISTRATIVO = "bloqueio_administrativo";
    public static final String SUSPENSO = "suspenso";
    public static final String CANCELADO = "cancelado";

    /** false quando o ERP nao respondeu: a consulta de massiva continua valendo, so sem este bloco. */
    @JsonProperty("disponivel")
    private boolean disponivel;

    /** Quantos contratos o cliente tem no ERP no total (com ou sem problema). */
    @JsonProperty("total_contratos")
    private Integer totalContratos;

    /** Ate 3 contratos com problema (bloqueios e suspensoes antes, depois os cancelamentos mais recentes). */
    @JsonProperty("problemas")
    private List<Problema> problemas;

    /** Quantos contratos com problema existem alem dos listados. */
    @JsonProperty("outros")
    private Integer outros;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Problema {
        @JsonProperty("contrato_id")
        private Long contratoId;
        @JsonProperty("situacao")
        private String situacao;
        /** Data (ISO) em que o contrato entrou nesta situacao. */
        @JsonProperty("desde")
        private String desde;
        @JsonProperty("dias")
        private Long dias;
        /** Motivo do cancelamento (ex.: "Inadimplência"); so em cancelados. */
        @JsonProperty("motivo")
        private String motivo;
    }
}
