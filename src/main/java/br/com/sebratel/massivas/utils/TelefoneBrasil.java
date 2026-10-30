package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Normaliza telefones brasileiros para comparar com os cadastrados no ERP, que guardam o numero com e sem
 * mascara, com ou sem +55 e, nos cadastros antigos, sem o nono digito.
 */
public final class TelefoneBrasil {

    private TelefoneBrasil() {
    }

    /**
     * Devolve as formas nacionais (DDD + numero) pelas quais o mesmo telefone pode estar cadastrado:
     * a recebida e, para celular, a variante com/sem o nono digito.
     *
     * @throws InvalidMassiveRequestException se nao sobrar um numero de 10 ou 11 digitos com DDD
     */
    public static Set<String> variantes(String bruto) {
        String digitos = bruto == null ? "" : bruto.replaceAll("\\D", "");
        if (digitos.length() > 10 && digitos.startsWith("0")) {
            digitos = digitos.substring(1);
        }
        if (digitos.length() > 11 && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new InvalidMassiveRequestException(
                    "Telefone invalido: informe DDD + numero (10 ou 11 digitos), com ou sem +55.");
        }
        String ddd = digitos.substring(0, 2);
        if (ddd.startsWith("0")) {
            throw new InvalidMassiveRequestException("Telefone invalido: DDD '" + ddd + "' inexistente.");
        }
        Set<String> variantes = new LinkedHashSet<>();
        variantes.add(digitos);
        if (digitos.length() == 11 && digitos.charAt(2) == '9') {
            variantes.add(ddd + digitos.substring(3));
        } else if (digitos.length() == 10 && "6789".indexOf(digitos.charAt(2)) >= 0) {
            variantes.add(ddd + "9" + digitos.substring(2));
        }
        return variantes;
    }

    /** Mascara para log e resposta: mantem DDD e os 2 ultimos digitos. */
    public static String mascarar(String bruto) {
        String digitos = bruto == null ? "" : bruto.replaceAll("\\D", "");
        if (digitos.length() < 6) {
            return "***";
        }
        return digitos.substring(0, 2) + "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 2);
    }
}
