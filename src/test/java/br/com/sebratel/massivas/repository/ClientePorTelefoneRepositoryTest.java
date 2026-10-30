package br.com.sebratel.massivas.repository;

import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientePorTelefoneRepositoryTest {

    @Test
    void aceitaColunasSimples() {
        assertDoesNotThrow(() -> new ClientePorTelefoneRepository(List.of("phone", " cell_phone_1 ")));
    }

    @Test
    void recusaIdentificadorComInjecaoDeSql() {
        assertThrows(IllegalStateException.class,
                () -> new ClientePorTelefoneRepository(List.of("phone) OR 1=1 --")));
        assertThrows(IllegalStateException.class,
                () -> new ClientePorTelefoneRepository(List.of("p.phone")));
        assertThrows(IllegalStateException.class,
                () -> new ClientePorTelefoneRepository(List.of("Phone")));
    }

    @Test
    void recusaListaVazia() {
        assertThrows(IllegalStateException.class, () -> new ClientePorTelefoneRepository(List.of(" ")));
    }
}
