package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AuthenticationSitesOutputDTO;
import br.com.sebratel.massivas.model.entity.AuthenticationSiteEntity;
import br.com.sebratel.massivas.repository.erp.AuthenticationSitesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationSitesServiceTest {

    @Mock
    private AuthenticationSitesRepository repository;

    @InjectMocks
    private AuthenticationSitesService service;

    @Test
    void executeShouldReturnListOfDTOsWhenTitleExists() {
        // Arrange
        String title = "Test Site";
        AuthenticationSiteEntity entity = new AuthenticationSiteEntity();
        entity.setTitle(title);
        entity.setCity("Test City");
        entity.setNeighborhood("Test Neighborhood");
        
        when(repository.findByTitle(title)).thenReturn(List.of(entity));

        // Act
        List<AuthenticationSitesOutputDTO> result = service.execute(title);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test City", result.get(0).getCity());
        verify(repository, times(1)).findByTitle(title);
    }

    @Test
    void executeShouldReturnEmptyListWhenTitleDoesNotExist() {
        // Arrange
        String title = "Non-existent";
        when(repository.findByTitle(title)).thenReturn(List.of());

        // Act
        List<AuthenticationSitesOutputDTO> result = service.execute(title);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByTitle(title);
    }
}
