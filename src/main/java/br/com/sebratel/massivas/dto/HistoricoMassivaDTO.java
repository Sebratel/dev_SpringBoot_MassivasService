package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/** Uma massiva recente (aberta ou encerrada na janela consultada) que atingiu contratos do cliente. */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HistoricoMassivaDTO {

    @JsonProperty("protocolo")
    private Long protocolo;

    @JsonProperty("titulo")
    private String titulo;

    /** Status gravado no historico (ex.: aberta, encerrada, cancelada). */
    @JsonProperty("status")
    private String status;

    /** Data e hora locais, ISO-8601 sem fuso (ex.: 2026-10-30T14:05:00). */
    @JsonProperty("aberta_em")
    private String abertaEm;

    @JsonProperty("encerrada_em")
    private String encerradaEm;

    @JsonProperty("descricao_encerramento")
    private String descricaoEncerramento;

    @JsonProperty("origem")
    private String origem;

    /** Quantos contratos do cliente estavam nesta massiva. */
    @JsonProperty("contratos_afetados")
    private Long contratosAfetados;
}
