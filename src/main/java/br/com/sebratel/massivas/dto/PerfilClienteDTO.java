package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * O que o cadastro do ERP e o historico de massivas sabem do cliente, para o resumo de recorrencia: quantos cadastros
 * (erp.people) e contratos, os telefones dele num formato unico e em quantas massivas ele esteve. Os telefones
 * servem aos outros servicos para achar protocolos e ligacoes; a camada que fala com a extensao nao os repassa.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfilClienteDTO {

    /** Registros em erp.people ligados a quem foi consultado (o mesmo CPF pode ter varios: matriz e filiais). */
    @JsonProperty("registros_people")
    private int registrosPeople;

    @JsonProperty("contratos")
    private int contratos;

    /** DDD + numero (10 ou 11 digitos), sem repeticao, de todas as colunas de telefone do cadastro. */
    @JsonProperty("telefones")
    private List<String> telefones;

    @JsonProperty("massivas")
    private MassivasDoCliente massivas;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MassivasDoCliente {
        @JsonProperty("ultimos_30_dias")
        private long ultimos30Dias;
        @JsonProperty("ultimos_90_dias")
        private long ultimos90Dias;
        @JsonProperty("ultimos_12_meses")
        private long ultimos12Meses;
        /** Abertura da massiva mais recente em que o cliente esteve (ISO-8601 sem fuso). */
        @JsonProperty("ultima_em")
        private String ultimaEm;
        /** Titulos mais comuns das massivas dos ultimos 12 meses. */
        @JsonProperty("assuntos")
        private List<Assunto> assuntos;
    }

    @Data
    @Builder
    public static class Assunto {
        @JsonProperty("titulo")
        private String titulo;
        @JsonProperty("vezes")
        private long vezes;
    }
}
