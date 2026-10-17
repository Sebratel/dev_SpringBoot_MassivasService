package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.AberturaProtocoloInfraInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoAssignmentDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.splitters.RecuperarTokenEllevenOutputDTO;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbrirProtocoloInfraNoEllevenApiServiceTest {

    @Mock
    private RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenService;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private AbrirProtocoloInfraNoEllevenApiService service;

    private AberturaProtocoloInfraInputDTO buildInput(String infraType) {
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Protocolo de Infra");
        assignment.setDescription("Mascara de infraestrutura");

        AberturaProtocoloInfraInputDTO input = new AberturaProtocoloInfraInputDTO();
        input.setInfraType(infraType);
        input.setPersonId(123L);
        input.setAuthenticationAccessPointCode("CAN-C-5562");
        input.setAssignment(assignment);
        return input;
    }

    @SuppressWarnings("unchecked")
    private void mockWebClientChain(Mono<AberturaRegistroMassivoOutputDTO> responseMono) {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(responseMono);
    }

    @Test
    void executarComSucesso() {
        AberturaProtocoloInfraInputDTO input = buildInput("cto_lo");

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        mockWebClientChain(Mono.just(expectedOutput));

        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        assertEquals(expectedOutput, result);
        verify(webClient).post();
        // O local de atendimento é carimbado pelo gateway.
        assertEquals(6, input.getAssignment().getCompanyPlaceId());
    }

    @Test
    void executarComTipoBackbone() {
        AberturaProtocoloInfraInputDTO input = buildInput("backbone");

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        mockWebClientChain(Mono.just(expectedOutput));

        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        assertEquals(expectedOutput, result);
    }

    @Test
    void executarComTipoInvalidoDeveLancarExcecao() {
        AberturaProtocoloInfraInputDTO input = buildInput("tipo_inexistente");

        assertThrows(IllegalArgumentException.class, () -> service.executar(input));
        verify(recuperarTokenService, never()).executar();
    }

    @Test
    void executarComErro401DevePropagarEInvalidarToken() {
        AberturaProtocoloInfraInputDTO input = buildInput("cto_sinal_alto");

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("invalid-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        WebClientResponseException.Unauthorized unauthorized = mock(WebClientResponseException.Unauthorized.class);
        mockWebClientChain(Mono.error(unauthorized));

        assertThrows(WebClientResponseException.Unauthorized.class, () -> service.executar(input));
        verify(recuperarTokenService).invalidateToken();
    }

    @Test
    void executarComWebClientResponseExceptionGeral() {
        AberturaProtocoloInfraInputDTO input = buildInput("cto_avariada");

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        WebClientResponseException badRequest = mock(WebClientResponseException.class);
        mockWebClientChain(Mono.error(badRequest));

        assertThrows(WebClientResponseException.class, () -> service.executar(input));
        verify(recuperarTokenService, never()).invalidateToken();
    }

    @Test
    void executarComExceptionGeral() {
        AberturaProtocoloInfraInputDTO input = buildInput("cto_lo");

        when(recuperarTokenService.executar()).thenThrow(new RuntimeException("Critical error"));

        assertThrows(RuntimeException.class, () -> service.executar(input));
    }
}
