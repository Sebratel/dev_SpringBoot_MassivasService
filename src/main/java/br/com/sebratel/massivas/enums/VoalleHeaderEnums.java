package br.com.sebratel.massivas.enums;

/**
 * Constantes para Headers customizados do ERP Voalle/Elleven.
 */
public final class VoalleHeaderEnums {

    // Construtor privado impede a instanciação da classe (Boa prática Sonar)
    private VoalleHeaderEnums() {
        throw new UnsupportedOperationException("Esta é uma classe de constantes e não deve ser instanciada.");
    }

    public static final String X_REQUESTED_WITH = "X-Requested-With";
    public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
}