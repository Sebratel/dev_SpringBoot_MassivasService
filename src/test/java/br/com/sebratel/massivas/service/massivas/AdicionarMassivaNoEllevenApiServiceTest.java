package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoAssignmentDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.splitters.RecuperarTokenEllevenOutputDTO;
import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import br.com.sebratel.massivas.model.Employee;
import br.com.sebratel.massivas.service.EmployeeService;
import java.util.Optional;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdicionarMassivaNoEllevenApiServiceTest {

    @Mock
    private RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenService;

    @Mock
    private WebClient webClient;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AdicionarMassivaNoEllevenApiService service;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Email", "test@sebratel.com.br");
        request.addHeader("X-User-Name", "Test+User");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void executarComSucesso() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(5);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        when(employeeService.hasB2BinInput(any())).thenReturn(false);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(expectedOutput));

        // Act
        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        // Assert
        assertEquals(expectedOutput, result);
        assertEquals(AdicionarMassivaNoEllevenApiService.NORMAL_EVENT_INCIDENT_TYPE_ID, input.getIncidentTypeId());
        verify(webClient).post();
    }

    @Test
    void executarComoEventoMassivoPelaQuantidade() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(16);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        when(employeeService.hasB2BinInput(any())).thenReturn(false);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(expectedOutput));

        // Act
        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        // Assert
        assertEquals(expectedOutput, result);
        assertEquals(AdicionarMassivaNoEllevenApiService.MASSIVE_EVENT_INCIDENT_TYPE_ID, input.getIncidentTypeId());
    }

    @Test
    void executarComoEventoMassivoPorTerB2B() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        when(employeeService.hasB2BinInput(any())).thenReturn(true);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(expectedOutput));

        // Act
        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        // Assert
        assertEquals(expectedOutput, result);
        assertEquals(AdicionarMassivaNoEllevenApiService.MASSIVE_EVENT_INCIDENT_TYPE_ID, input.getIncidentTypeId());
    }

    @Test
    void executarComErro401DevePropagarExcecao() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("invalid-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);
        when(employeeService.hasB2BinInput(any())).thenReturn(false);

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

        WebClientResponseException.Unauthorized unauthorizedException = mock(WebClientResponseException.Unauthorized.class);
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.error(unauthorizedException));

        // Act & Assert
        assertThrows(WebClientResponseException.Unauthorized.class, () -> service.executar(input));
    }

    @Test
    void executarComWebClientResponseExceptionGeral() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);
        when(employeeService.hasB2BinInput(any())).thenReturn(false);

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

        WebClientResponseException badRequestException = mock(WebClientResponseException.class);
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.error(badRequestException));

        // Act & Assert
        assertThrows(WebClientResponseException.class, () -> service.executar(input));
    }

    @Test
    void executarComExceptionGeral() {
        // Arrange
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        when(recuperarTokenService.executar()).thenThrow(new RuntimeException("Critical error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.executar(input));
    }
    @Test
    void executarComAffectedUsersNullDeveRecusarSemChamarElleven() {
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(null);
        input.setAffectedUsersQuantity(0);

        InvalidMassiveRequestException ex = assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));

        assertTrue(ex.getMessage().contains("Quantidade de usuários afetados inválida"));
        assertTrue(ex.getMessage().contains("Nenhum protocolo foi aberto no Voalle"));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComZeroUsuariosAfetadosDeveRecusarSemChamarElleven() {
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(new ArrayList<>());
        input.setAffectedUsersQuantity(0);

        InvalidMassiveRequestException ex = assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));

        assertTrue(ex.getMessage().contains("affectedUsersQuantity=0"));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComQuantidadeZeroMasListaPreenchidaDeveRecusar() {
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(0);

        assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComQuantidadeInformadaEListaVaziaDeveAbrir() {
        // Fluxo da plataforma: abre só com a quantidade; os afetados vão depois no POST de afetados.
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(new ArrayList<>());
        input.setAffectedUsersQuantity(3);

        RecuperarTokenEllevenOutputDTO tokenOutput = new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all");
        when(recuperarTokenService.executar()).thenReturn(tokenOutput);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        AberturaRegistroMassivoOutputDTO expectedOutput = new AberturaRegistroMassivoOutputDTO();
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(expectedOutput));

        AberturaRegistroMassivoOutputDTO result = service.executar(input);

        assertEquals(expectedOutput, result);
    }

    @Test
    void executarComUsuarioSemJwtDeveRecusarSemChamarElleven() {
        RequestContextHolder.resetRequestAttributes();
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        InvalidMassiveRequestException ex = assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));

        assertTrue(ex.getMessage().contains("Usuário solicitante inválido"));
        assertTrue(ex.getMessage().contains("Nenhum protocolo foi aberto no Voalle"));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComJwtSemClaimsResolveSolicitantePeloPersonId() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setPersonId(141085L);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);
        when(employeeService.findEmployeeByPersonId(141085L))
                .thenReturn(Optional.of(new Employee("fulano@sebratel.com.br", "Fulano")));
        when(employeeService.hasB2BinInput(any())).thenReturn(false);
        when(recuperarTokenService.executar()).thenReturn(new RecuperarTokenEllevenOutputDTO("fake-token", 3600, "Bearer", "all"));

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq(HttpHeaders.AUTHORIZATION), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AberturaRegistroMassivoOutputDTO.class)).thenReturn(Mono.just(new AberturaRegistroMassivoOutputDTO()));

        service.executar(input);

        assertTrue(input.getAssignment().getDescription().endsWith("Fulano(fulano@sebratel.com.br)"));
        verify(webClient).post();
    }

    @Test
    void executarComJwtSemClaimsEPersonIdSemCadastroDeveRecusar() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setPersonId(999L);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);
        when(employeeService.findEmployeeByPersonId(999L)).thenReturn(Optional.empty());

        assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComJwtSemClaimsEFalhaNaConsultaDeveRecusar() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setPersonId(141085L);
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);
        when(employeeService.findEmployeeByPersonId(141085L)).thenThrow(new RuntimeException("db down"));

        assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    @Test
    void executarComJwtSemEmailDeveRecusar() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        AberturaRegistroMassivoInputDTO input = inputBase();
        input.setAffectedUsers(umAfetado());
        input.setAffectedUsersQuantity(1);

        assertThrows(InvalidMassiveRequestException.class, () -> service.executar(input));
        verifyNoInteractions(recuperarTokenService, webClient);
    }

    private static AberturaRegistroMassivoInputDTO inputBase() {
        AberturaRegistroMassivoInputDTO input = new AberturaRegistroMassivoInputDTO();
        AberturaRegistroMassivoAssignmentDTO assignment = new AberturaRegistroMassivoAssignmentDTO();
        assignment.setTitle("Test Title");
        assignment.setDescription("Test Description");
        input.setAssignment(assignment);
        return input;
    }

    private static List<AffectedUsersEntity> umAfetado() {
        return new ArrayList<>(List.of(new AffectedUsersEntity()));
    }

}
