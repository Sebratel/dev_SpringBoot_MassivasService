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

    /** Quantos eventos ativos o AutoISP tem no total (so para conferir que a lista veio; nao e do cliente). */
    @JsonProperty("ativos_no_autoisp")
    private Integer ativosNoAutoisp;

    @JsonProperty("eventos")
    private List<Evento> eventos;

    /** Eventos ja encerrados nas ultimas {@code recentes_horas} horas que atingiram contratos do cliente. */
    @JsonProperty("recentes")
    private List<Evento> recentes;

    @JsonProperty("recentes_horas")
    private Integer recentesHoras;

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
        /** Fim do evento; ausente enquanto ele esta ativo. */
        @JsonProperty("fim")
        private String fim;
        @JsonProperty("contratos_afetados")
        private Long contratosAfetados;
    }
}
