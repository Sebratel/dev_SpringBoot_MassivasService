package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoBrasilTest {

    @Test
    @DisplayName("CPF e CNPJ saem so com digitos")
    void normalizaCpfECnpj() {
        assertEquals("12345678901", DocumentoBrasil.normalizar("123.456.789-01"));
        assertEquals("12345678000199", DocumentoBrasil.normalizar("12.345.678/0001-99"));
    }

    @Test
    @DisplayName("Tamanho diferente de 11 ou 14 digitos e recusado")
    void recusaTamanhoInvalido() {
        assertThrows(InvalidMassiveRequestException.class, () -> DocumentoBrasil.normalizar("123"));
        assertThrows(InvalidMassiveRequestException.class, () -> DocumentoBrasil.normalizar(null));
        assertThrows(InvalidMassiveRequestException.class, () -> DocumentoBrasil.normalizar("1234567890123"));
    }

    @Test
    @DisplayName("A mascara mantem 3 primeiros e 2 ultimos digitos")
    void mascara() {
        assertEquals("123******01", DocumentoBrasil.mascarar("12345678901"));
        assertEquals("123*********99", DocumentoBrasil.mascarar("12345678000199"));
    }
}
