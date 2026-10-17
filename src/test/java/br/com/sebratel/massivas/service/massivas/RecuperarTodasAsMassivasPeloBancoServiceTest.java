package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivasBFFOutputDTO;
import br.com.sebratel.massivas.repository.erp.massivas.RecuperarTodasAsMassivasPeloBancoRepository;
import br.com.sebratel.massivas.repository.erp.projections.RecuperarTodasAsMassivasProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperarTodasAsMassivasPeloBancoServiceTest {

    @Mock
    private RecuperarTodasAsMassivasPeloBancoRepository repository;

    @InjectMocks
    private RecuperarTodasAsMassivasPeloBancoService service;

    @Test
    void executarComSucesso() {
        // Arrange
        RecuperarTodasAsMassivasProjection projection = mock(RecuperarTodasAsMassivasProjection.class);
        when(projection.getID()).thenReturn(1L);
        when(projection.getCRIACAO()).thenReturn(LocalDateTime.now());
        when(projection.getPROTOCOLO()).thenReturn("PROT123");
        when(projection.getSTATUS()).thenReturn("Aberto");

        when(repository.findActiveAssignments()).thenReturn(List.of(projection));

        // Act
        List<MassivasBFFOutputDTO> result = service.executar();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("PROT123", result.get(0).protocolo());
        assertEquals("Aberto", result.get(0).status());
    }

    @Test
    void executarListaVazia() {
        // Arrange
        when(repository.findActiveAssignments()).thenReturn(List.of());

        // Act
        List<MassivasBFFOutputDTO> result = service.executar();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
