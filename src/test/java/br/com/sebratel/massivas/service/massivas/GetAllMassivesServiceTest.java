package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.EllevenApiResponseDTO;
import br.com.sebratel.massivas.utils.GetToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllMassivesServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.Builder webClientBuilder;

    private GetAllMassivesService service;

    private void setupService() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        service = new GetAllMassivesService(webClientBuilder);
    }

    @Test
    void getAllSolicitationsComSucesso() {
        try (MockedStatic<GetToken> mockedGetToken = mockStatic(GetToken.class)) {
            mockedGetToken.when(GetToken::retrieve).thenReturn("Bearer fake-token");
            setupService();

            WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            when(webClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.header(anyString(), any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

            EllevenApiResponseDTO expectedOutput = new EllevenApiResponseDTO();
            when(responseSpec.bodyToMono(EllevenApiResponseDTO.class)).thenReturn(Mono.just(expectedOutput));

            EllevenApiResponseDTO result = service.getAllSolicitations();

            assertEquals(expectedOutput, result);
            verify(webClient).get();
        }
    }

    @Test
    void getAllSolicitationsComErro() {
        try (MockedStatic<GetToken> mockedGetToken = mockStatic(GetToken.class)) {
            mockedGetToken.when(GetToken::retrieve).thenReturn("Bearer fake-token");
            setupService();

            when(webClient.get()).thenThrow(new RuntimeException("API error"));

            assertThrows(RuntimeException.class, () -> service.getAllSolicitations());
        }
    }
}
