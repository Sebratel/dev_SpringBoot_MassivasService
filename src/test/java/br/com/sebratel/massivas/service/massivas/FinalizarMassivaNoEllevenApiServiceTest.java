package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.splitters.RecuperarTokenEllevenOutputDTO;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalizarMassivaNoEllevenApiServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenService;

    @Mock
    private FinishLinkedProtocolsService finishLinkedProtocolsService;


    @InjectMocks
    private FinalizarMassivaNoEllevenApiService service;

    private SecurityContext securityContext;
    private Authentication authentication;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        jwt = mock(Jwt.class);

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockJwt() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("email")).thenReturn("test@sebratel.com.br");
        when(jwt.getClaim("name")).thenReturn("Test User");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executarComSucesso() {
        // Arrange
        mockJwt();
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();
        input.setDescription("Closing description");

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient mutatedWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.mutate()).thenReturn(builder);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(mutatedWebClient);
        when(mutatedWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        FinalizarRegistroMassivoOutputDTO expectedOutput = FinalizarRegistroMassivoOutputDTO.builder().success(true).build();
        when(responseSpec.bodyToMono(FinalizarRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(expectedOutput));

        // Act
        FinalizarRegistroMassivoOutputDTO result = service.executar(input);
        verify(finishLinkedProtocolsService).executar(any());


        // Assert
        assertEquals(expectedOutput, result);
        verify(mutatedWebClient).post();
    }

    @Test
    void executarComErroToken() {
        // Arrange
        FinalizaRegistroMassivoInputDTO input = new FinalizaRegistroMassivoInputDTO();
        when(recuperarTokenService.executar()).thenThrow(new RuntimeException("Token error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.executar(input));
    }
}
