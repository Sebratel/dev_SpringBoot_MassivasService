package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.EllevenCredentialsDTO;
import br.com.sebratel.massivas.dto.splitters.RecuperarTokenEllevenOutputDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecuperarTokenDoUsuarioIntegradorEllevenServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private EllevenCredentialsDTO credentials;

    @InjectMocks
    private RecuperarTokenDoUsuarioIntegradorEllevenService service;

    @Test
    void executarShouldReturnTokenWhenApiSucceeds() {
        // Arrange
        RecuperarTokenEllevenOutputDTO expectedResponse = new RecuperarTokenEllevenOutputDTO("token", 3600, "bearer", "scope");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", "id");
        when(credentials.toFormData()).thenReturn(formData);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(RecuperarTokenEllevenOutputDTO.class)).thenReturn(Mono.just(expectedResponse));

        // Act
        RecuperarTokenEllevenOutputDTO result = service.executar();

        // Assert
        assertNotNull(result);
        assertEquals("token", result.accessToken());
    }
}
