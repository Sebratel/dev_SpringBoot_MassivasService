package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Alteracoes de rede ativas no AutoISP que atingem os contratos do cliente consultado. So o que o atendente
 * precisa para falar com o cliente: tipo, status, inicio e quantos contratos dele estao no evento.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutoIspSituacaoDTO {

    /** false quando o AutoISP nao respondeu: a consulta de massiva continua valendo, so sem este bloco. */
    @JsonProperty("disponivel")
    private boolean disponivel;

    @JsonProperty("eventos")
    private List<Evento> eventos;

    @Data
    @Builder
    public static class Evento {
        @JsonProperty("id")
        private Long id;
        @JsonProperty("tipo")
        private String tipo;
        @JsonProperty("status")
        private String status;
        @JsonProperty("inicio")
        private String inicio;
        @JsonProperty("contratos_afetados")
        private Long contratosAfetados;
    }
}
