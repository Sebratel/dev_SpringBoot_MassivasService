package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.ConfirmacaoEllevenDTO;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaInputDTO;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaOutputDTO;
import br.com.sebratel.massivas.dto.massivas.MassivaCriadaOutputDTO;
import br.com.sebratel.massivas.exceptions.IntegrationEllevenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdicionarMassivaNoEllevenServiceTest {

    @Mock
    private WebClient webClient;

    @InjectMocks
    private AdicionarMassivaNoEllevenService service;

    @Test
    void salvarNoBancoERPComSucesso() {
        // Arrange
        CriacaoDeMassivaInputDTO input = CriacaoDeMassivaInputDTO.builder()
                .startDate(LocalDate.now())
                .startTime(LocalTime.now())
                .accessPointIds(new Integer[]{1})
                .slotOlt(new Integer[]{1})
                .portaOlt(new Integer[]{1})
                .addressListId(new Integer[]{1})
                .assignmentDescription("Test Description")
                .maintenanceDate(LocalDate.now())
                .maintenanceTime(LocalTime.now())
                .cookieString("fake-cookie")
                .companyPlaceId(1)
                .assignmentTypeId(1)
                .build();

        MassivaCriadaOutputDTO creationOutput = new MassivaCriadaOutputDTO("true", "123");
        ConfirmacaoEllevenDTO impactOutput = new ConfirmacaoEllevenDTO(true, "Impact Set");
        ConfirmacaoEllevenDTO finalOutput = new ConfirmacaoEllevenDTO(true, "Success. Protocolo: PROT123");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MassivaCriadaOutputDTO.class)).thenReturn(Mono.just(creationOutput));
        when(responseSpec.bodyToMono(ConfirmacaoEllevenDTO.class))
                .thenReturn(Mono.just(impactOutput))
                .thenReturn(Mono.just(finalOutput));

        // Act
        CriacaoDeMassivaOutputDTO result = service.salvarNoBancoERP(input);

        // Assert
        assertNotNull(result);
        assertEquals("123", result.getId());
        assertEquals("PROT123", result.getProtocolo());
        verify(webClient, times(3)).post();
    }

    @Test
    void salvarNoBancoERPComListasNulas() {
        // Arrange
        CriacaoDeMassivaInputDTO input = CriacaoDeMassivaInputDTO.builder()
                .startDate(LocalDate.now())
                .startTime(LocalTime.now())
                .accessPointIds(null)
                .slotOlt(null)
                .portaOlt(null)
                .addressListId(null)
                .assignmentDescription("Test Description")
                .maintenanceDate(LocalDate.now())
                .maintenanceTime(LocalTime.now())
                .cookieString("fake-cookie")
                .companyPlaceId(1)
                .assignmentTypeId(1)
                .build();

        MassivaCriadaOutputDTO creationOutput = new MassivaCriadaOutputDTO("true", "123");
        ConfirmacaoEllevenDTO impactOutput = new ConfirmacaoEllevenDTO(true, "Impact Set");
        ConfirmacaoEllevenDTO finalOutput = new ConfirmacaoEllevenDTO(true, "Success. Protocolo: PROT123");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MassivaCriadaOutputDTO.class)).thenReturn(Mono.just(creationOutput));
        when(responseSpec.bodyToMono(ConfirmacaoEllevenDTO.class))
                .thenReturn(Mono.just(impactOutput))
                .thenReturn(Mono.just(finalOutput));

        // Act
        CriacaoDeMassivaOutputDTO result = service.salvarNoBancoERP(input);

        // Assert
        assertNotNull(result);
        assertEquals("123", result.getId());
        verify(webClient, times(3)).post();
    }

    @Test
    void salvarNoBancoERPFalhaNaValidacao() {
        // Arrange
        CriacaoDeMassivaInputDTO input = CriacaoDeMassivaInputDTO.builder()
                .startDate(LocalDate.now())
                .startTime(LocalTime.now())
                .accessPointIds(new Integer[]{1})
                .slotOlt(new Integer[]{1})
                .portaOlt(new Integer[]{1})
                .addressListId(new Integer[]{1})
                .cookieString("fake-cookie")
                .build();

        MassivaCriadaOutputDTO creationOutput = new MassivaCriadaOutputDTO("true", "123");
        ConfirmacaoEllevenDTO impactOutput = new ConfirmacaoEllevenDTO(false, "Error");

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MassivaCriadaOutputDTO.class)).thenReturn(Mono.just(creationOutput));
        when(responseSpec.bodyToMono(ConfirmacaoEllevenDTO.class)).thenReturn(Mono.just(impactOutput));

        // Act & Assert
        assertThrows(IntegrationEllevenException.class, () -> service.salvarNoBancoERP(input));
    }

    @Test
    void salvarNoBancoERPComMensagemFinalNula() {
        // Arrange
        CriacaoDeMassivaInputDTO input = CriacaoDeMassivaInputDTO.builder()
                .startDate(LocalDate.now())
                .startTime(LocalTime.now())
                .accessPointIds(new Integer[]{1})
                .slotOlt(new Integer[]{1})
                .portaOlt(new Integer[]{1})
                .addressListId(new Integer[]{1})
                .assignmentDescription("Test Description")
                .maintenanceDate(LocalDate.now())
                .maintenanceTime(LocalTime.now())
                .cookieString("fake-cookie")
                .companyPlaceId(1)
                .assignmentTypeId(1)
                .build();

        MassivaCriadaOutputDTO creationOutput = new MassivaCriadaOutputDTO("true", "123");
        ConfirmacaoEllevenDTO impactOutput = new ConfirmacaoEllevenDTO(true, "Impact Set");
        ConfirmacaoEllevenDTO finalOutput = new ConfirmacaoEllevenDTO(true, null);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(MassivaCriadaOutputDTO.class)).thenReturn(Mono.just(creationOutput));
        when(responseSpec.bodyToMono(ConfirmacaoEllevenDTO.class))
                .thenReturn(Mono.just(impactOutput))
                .thenReturn(Mono.just(finalOutput));

        // Act & Assert
        // The assert confFinal.getMessage() != null throws AssertionError if enabled,
        // otherwise it would throw NullPointerException at split.
        assertThrows(Throwable.class, () -> service.salvarNoBancoERP(input));
    }
}
