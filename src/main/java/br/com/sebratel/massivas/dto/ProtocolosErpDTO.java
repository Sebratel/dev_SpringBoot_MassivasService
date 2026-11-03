package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Protocolos, OS e visitas em aberto do cliente no ERP (evita o atendente abrir um chamado repetido). */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolosErpDTO {

    /** false quando o ERP nao respondeu: a consulta de massiva continua valendo, so sem este bloco. */
    @JsonProperty("disponivel")
    private boolean disponivel;

    /** Quantos protocolos abertos o cliente tem no total. */
    @JsonProperty("total")
    private Integer total;

    /** Os mais recentes (ate 5). */
    @JsonProperty("itens")
    private List<Item> itens;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        @JsonProperty("protocolo")
        private Long protocolo;
        @JsonProperty("tipo")
        private String tipo;
        /** Situacao no ERP (Agendado, Andamento, Visita 1...). */
        @JsonProperty("situacao")
        private String situacao;
        @JsonProperty("aberto_em")
        private String abertoEm;
        @JsonProperty("ha_dias")
        private Long haDias;
        /** Tipo de suporte tecnico, manutencao, CGR/NOC ou visita: o atendente deve evitar abrir outro. */
        @JsonProperty("tecnico")
        private boolean tecnico;
        /** Aberto ha mais tempo que o corte (180 dias): costuma ser protocolo esquecido no ERP. */
        @JsonProperty("antigo")
        private boolean antigo;
        /** Proxima visita agendada (data/hora locais ISO), quando o ERP a registrou. */
        @JsonProperty("visita_em")
        private String visitaEm;
    }
}
