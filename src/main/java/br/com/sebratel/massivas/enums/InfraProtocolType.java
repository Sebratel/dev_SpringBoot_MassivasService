package br.com.sebratel.massivas.enums;

import java.util.Arrays;

/**
 * Tipos de protocolo de infraestrutura que podem ser abertos junto com uma massiva.
 * Cada tipo carrega as constantes que o diferenciam no ERP Voalle (catálogo de serviço,
 * tipo de solicitação e a Categoria 3). As demais categorias/parâmetros são compartilhados
 * e ficam em {@code AbrirProtocoloInfraNoEllevenApiService}.
 *
 * IMPORTANTE: {@code category3} é a "etiqueta" (tag) da categoria no Voalle — NÃO é o id nem
 * o nome. Sem a etiqueta correta o ERP responde code 1000
 * "Nenhuma etiqueta foi encontrada para consulta das categorias".
 */
public enum InfraProtocolType {

    CTO_LO("cto_lo", 1114, 1186, "3a25753b"),
    CTO_SINAL_ALTO("cto_sinal_alto", 1115, 1187, "1b3681c5"),
    CTO_AVARIADA("cto_avariada", 1116, 1188, "d96ee69d"),
    BACKBONE("backbone", 1121, 1193, "676df99b");

    private final String code;
    private final int catalogServiceId;
    private final int incidentTypeId;
    private final String category3;

    InfraProtocolType(String code, int catalogServiceId, int incidentTypeId, String category3) {
        this.code = code;
        this.catalogServiceId = catalogServiceId;
        this.incidentTypeId = incidentTypeId;
        this.category3 = category3;
    }

    public String getCode() {
        return code;
    }

    public int getCatalogServiceId() {
        return catalogServiceId;
    }

    public int getIncidentTypeId() {
        return incidentTypeId;
    }

    public String getCategory3() {
        return category3;
    }

    /** Resolve o tipo a partir do código enviado pelo frontend (ex.: "cto_lo"). */
    public static InfraProtocolType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de protocolo de infraestrutura invalido: " + code));
    }
}
