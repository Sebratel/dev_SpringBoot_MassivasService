package br.com.sebratel.massivas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Situacao de massiva de quem ligou, a partir do telefone. Nao devolve nome nem endereco: o alerta so precisa
 * saber se ha massiva, o motivo e a previsao.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClienteMassivaPorTelefoneOutputDTO {

    public static final String EM_MASSIVA = "em_massiva";
    public static final String SEM_MASSIVA = "sem_massiva";
    public static final String CLIENTE_NAO_ENCONTRADO = "cliente_nao_encontrado";
    /** Telefone em cadastro de muitos clientes (ex.: o da propria empresa): nao identifica quem ligou. */
    public static final String TELEFONE_COMPARTILHADO = "telefone_compartilhado";

    /** em_massiva, sem_massiva, cliente_nao_encontrado ou telefone_compartilhado. */
    @JsonProperty("status")
    private String status;

    @JsonProperty("em_massiva")
    private boolean emMassiva;

    /** Telefone consultado, mascarado. */
    @JsonProperty("telefone")
    private String telefone;

    /** Documento consultado (CPF/CNPJ), mascarado. So na busca por documento. */
    @JsonProperty("documento")
    private String documento;

    /** Quantos clientes diferentes usam este telefone (mais de um exige confirmacao do atendente). */
    @JsonProperty("clientes_encontrados")
    private int clientesEncontrados;

    @JsonProperty("contratos_consultados")
    private int contratosConsultados;

    /** Menor previsao entre os contratos em massiva, arredondada para cima, em horas. */
    @JsonProperty("previsao_horas")
    private Long previsaoHoras;

    /** Hora do dia da previsao do contrato com menor previsao. */
    @JsonProperty("previsao_hora")
    private Long previsaoHora;

    /** Ha evento de rede ativo no AutoISP atingindo contrato do cliente (independe de massiva). */
    @JsonProperty("em_autoisp")
    private boolean emAutoisp;

    /** Eventos do AutoISP; ausente quando o AutoISP nao esta configurado neste servidor. */
    @JsonProperty("autoisp")
    private AutoIspSituacaoDTO autoisp;

    @JsonProperty("motivo")
    private String motivo;

    @JsonProperty("contratos_em_massiva")
    private List<ContratoMassiva> contratosEmMassiva;

    @Data
    @Builder
    public static class ContratoMassiva {
        @JsonProperty("contrato_id")
        private Long contratoId;
        @JsonProperty("cliente_id")
        private Long clienteId;
        @JsonProperty("motivo")
        private String motivo;
        @JsonProperty("previsao_horas")
        private Long previsaoHoras;
        @JsonProperty("previsao_hora")
        private Long previsaoHora;
    }
}
