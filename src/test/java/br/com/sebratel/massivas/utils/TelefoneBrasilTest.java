package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelefoneBrasilTest {

    @Test
    void celularComNonoDigitoGeraTambemAVarianteSemNono() {
        assertEquals(List.of("51995664811", "5195664811"),
                List.copyOf(TelefoneBrasil.variantes("51995664811")));
    }

    @Test
    void celularAntigoSemNonoGeraTambemAVarianteComNono() {
        assertEquals(List.of("5195664811", "51995664811"),
                List.copyOf(TelefoneBrasil.variantes("(51) 9566-4811")));
    }

    @Test
    void removeMascaraCodigoDoPaisEZeroDeTronco() {
        assertEquals("51995664811", TelefoneBrasil.variantes("+55 (51) 99566-4811").iterator().next());
        assertEquals("51995664811", TelefoneBrasil.variantes("055 51 995664811").iterator().next());
        assertEquals("51995664811", TelefoneBrasil.variantes("051995664811").iterator().next());
    }

    @Test
    void fixoNaoGeraVarianteComNono() {
        assertEquals(List.of("5133334444"), List.copyOf(TelefoneBrasil.variantes("5133334444")));
    }

    @Test
    void recusaNumeroSemDddOuComTamanhoInvalido() {
        assertThrows(InvalidMassiveRequestException.class, () -> TelefoneBrasil.variantes("995664811"));
        assertThrows(InvalidMassiveRequestException.class, () -> TelefoneBrasil.variantes("1234"));
        assertThrows(InvalidMassiveRequestException.class, () -> TelefoneBrasil.variantes(""));
        assertThrows(InvalidMassiveRequestException.class, () -> TelefoneBrasil.variantes(null));
        assertThrows(InvalidMassiveRequestException.class, () -> TelefoneBrasil.variantes("0095664811"));
    }

    @Test
    void mascaraMantemDddEDoisUltimosDigitos() {
        assertEquals("51*******11", TelefoneBrasil.mascarar("(51) 99566-4811"));
        assertEquals("***", TelefoneBrasil.mascarar("123"));
    }
}
