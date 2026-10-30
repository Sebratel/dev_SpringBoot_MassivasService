package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;

/**
 * CPF (11 digitos) ou CNPJ (14) digitado pelo atendente, com ou sem pontuacao. O ERP guarda so os digitos.
 */
public final class DocumentoBrasil {

    private DocumentoBrasil() {
    }

    /**
     * Apenas os digitos do CPF/CNPJ.
     *
     * @throws InvalidMassiveRequestException (400) se nao tiver 11 ou 14 digitos
     */
    public static String normalizar(String documento) {
        String digitos = documento == null ? "" : documento.replaceAll("[^0-9]", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new InvalidMassiveRequestException("Documento invalido: informe CPF (11 digitos) ou CNPJ (14 digitos).");
        }
        return digitos;
    }

    /** Mantem os 3 primeiros e os 2 ultimos digitos (para conferir sem expor o documento inteiro). */
    public static String mascarar(String documento) {
        String digitos = documento == null ? "" : documento.replaceAll("[^0-9]", "");
        if (digitos.length() < 6) {
            return "***";
        }
        return digitos.substring(0, 3) + "*".repeat(digitos.length() - 5) + digitos.substring(digitos.length() - 2);
    }
}
