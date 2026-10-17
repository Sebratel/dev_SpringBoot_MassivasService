package br.com.sebratel.massivas.exceptions;

import br.com.sebratel.massivas.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void shouldHandleFeatureNotImplementedException() {
        FeatureNotImplementedException ex = new FeatureNotImplementedException("Not implemented");
        ResponseEntity<ApiError> response = exceptionHandler.handleNotImplemented(ex, request);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        assertEquals("Not implemented", response.getBody().getMessage());
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<ApiError> response = exceptionHandler.handleNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void shouldHandleIntegrationEllevenException() {
        IntegrationEllevenException ex = new IntegrationEllevenException("Integration error");
        ResponseEntity<ApiError> response = exceptionHandler.handleIntegrationElleven(ex, request);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("Integration error", response.getBody().getMessage());
    }

    @Test
    void shouldHandleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = mock(DataIntegrityViolationException.class);
        Throwable rootCause = new Throwable("Column 'field' cannot be null");
        when(ex.getRootCause()).thenReturn(rootCause);
        
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleDataIntegrityViolation(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().getErrors());
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new Exception("Generic error");
        ResponseEntity<ApiError> response = exceptionHandler.handleGenericException(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocorreu um erro inesperado no servidor.", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
        assertTrue(response.getBody().getDetails().get(0).contains("Generic error"));
    }

    @Test
    void shouldIncludeRootCauseInGenericExceptionDetails() {
        Exception root = new IllegalStateException("conexao recusada");
        Exception ex = new RuntimeException("falha ao processar", root);
        ResponseEntity<ApiError> response = exceptionHandler.handleGenericException(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(2, response.getBody().getDetails().size());
        assertTrue(response.getBody().getDetails().get(1).contains("conexao recusada"));
    }
}
