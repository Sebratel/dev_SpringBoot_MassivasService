package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.exceptions.InvalidMassiveRequestException;
import br.com.sebratel.massivas.model.Employee;
import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import br.com.sebratel.massivas.service.EmployeeService;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import br.com.sebratel.massivas.utils.JwtInformation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class AdicionarMassivaNoEllevenApiService {

    public static final int NORMAL_EVENT_INCIDENT_TYPE_ID = 1265;
    public static final int NORMAL_EVENT_CATALOG_SERVICE_ID = 1179;
    public static final String NORMAL_EVENT_CATEGORY_SOCILITATION = "MASSIVAS - 002";
    public static final int MASSIVE_EVENT_INCIDENT_TYPE_ID = 1257;
    public static final int MASSIVE_EVENT_CATALOG_SERVICE_ID = 1173;
    public static final String MASSIVE_EVENT_CATEGORY_SOCILITATION = "MASSIVAS - 001";

    private final RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService;
    private final WebClient webClient;
    private final EmployeeService employeeService;

    @Autowired
    public AdicionarMassivaNoEllevenApiService(RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService, WebClient webClient, EmployeeService employeeService) {
        this.recuperarTokenDoUsuarioIntegradorEllevenService = recuperarTokenDoUsuarioIntegradorEllevenService;
        this.webClient = webClient;
        this.employeeService = employeeService;
    }

    public AberturaRegistroMassivoOutputDTO executar(@Valid AberturaRegistroMassivoInputDTO input) {
        Employee x = JwtInformation.retrieveUserData();
        String email = x.email();
        String name = x.name();

        if (isBlank(name) || isBlank(email)) {
            Employee fallback = resolverSolicitantePeloPersonId(input.getPersonId());
            if (fallback != null) {
                email = fallback.email();
                name = fallback.name();
            }
        }

        validarRequisicao(name, email, input);

        input.getAssignment().setDescription(input.getAssignment().getDescription() + " - " + name + "("+ email +")");

        log.info("[MASSIVA] Usuário {} ({}) solicitando abertura de registro: '{}'. Total de usuários afetados: {}",
                name, email, input.getAssignment().getTitle(), input.getAffectedUsersQuantity());

        try {
            log.debug("[MASSIVA] Buscando token de integração...");
            String token = recuperarTokenDoUsuarioIntegradorEllevenService.executar().accessToken();
            String url = "https://erp.sebratel.net.br:45715/external/integrations/thirdparty/opendetailedsolicitation";


            input = this.decideForIncidentOrMassiveEvent(input);

            log.info("[MASSIVA] Regra de negócio aplicada: IncidentTypeId definido como {}",
                    input.getIncidentTypeId());

            log.info("[MASSIVA] Enviando payload para Elleven API: {}", url);

            // Medindo o tempo de resposta da API externa (opcional, mas muito útil)
            long startTime = System.currentTimeMillis();

            AberturaRegistroMassivoOutputDTO response = webClient
                    .post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(AberturaRegistroMassivoOutputDTO.class)
                    .block();

            long duration = System.currentTimeMillis() - startTime;

            log.info("[MASSIVA] Sucesso! Registro criado no Elleven em {}ms. Resposta: {}", duration, response);
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("[MASSIVA-ERRO] Erro de autenticação (401) na API Elleven. Invalidando token em cache.");
            recuperarTokenDoUsuarioIntegradorEllevenService.invalidateToken();
            throw e;
        } catch (WebClientResponseException e) {
            // Log específico para erros de API (4xx ou 5xx) com o corpo do erro da API
            log.error("[MASSIVA-ERRO] Falha na API Elleven. Status: {}. Response Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[MASSIVA-ERRO] Falha crítica ao processar massiva. Título: {}. Causa: {}",
                    input.getAssignment().getTitle(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Contingência: JWT sem claims 'name'/'email' (ex.: token de integração). Resolve o solicitante
     * pelo personId informado no corpo. Falha na consulta não derruba o fluxo: a validação recusa em seguida.
     */
    private Employee resolverSolicitantePeloPersonId(Long personId) {
        try {
            Employee employee = employeeService.findEmployeeByPersonId(personId).orElse(null);
            if (employee != null) {
                log.warn("[MASSIVA] JWT sem name/email; solicitante resolvido pelo personId {}: {} ({})",
                        personId, employee.name(), employee.email());
            } else {
                log.warn("[MASSIVA] JWT sem name/email e personId {} sem nome/e-mail no ERP.", personId);
            }
            return employee;
        } catch (Exception e) {
            log.error("[MASSIVA] Falha ao resolver solicitante pelo personId {}: {}", personId, e.getMessage());
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Valida antes de qualquer chamada ao Elleven, para não abrir protocolo no Voalle
     * que depois precisaria ser finalizado com mensagem de erro (e propagada ao Splitters).
     */
    private void validarRequisicao(String name, String email, AberturaRegistroMassivoInputDTO input) {
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            log.warn("[MASSIVA] Abertura recusada: usuário não identificado (nome='{}', email='{}'). Título: {}",
                    name, email, input.getAssignment().getTitle());
            throw new InvalidMassiveRequestException(
                    "Usuário solicitante inválido: não foi possível identificar nome e e-mail a partir do token JWT. "
                            + "A massiva só pode ser aberta por um usuário autenticado com JWT válido (claims 'name' e 'email'). "
                            + "Nenhum protocolo foi aberto no Voalle.");
        }

        // Só a QUANTIDADE é obrigatória na abertura. A plataforma (paridade com o app Flutter)
        // abre em duas etapas: este POST vai com affectedUsers vazio + affectedUsersQuantity, e os
        // afetados são enviados depois no POST de afetados. Exigir a lista aqui recusava toda
        // massiva aberta pela plataforma.
        int quantidade = input.getAffectedUsersQuantity();
        int listados = input.getAffectedUsers() != null ? input.getAffectedUsers().size() : 0;
        if (quantidade <= 0) {
            log.warn("[MASSIVA] Abertura recusada: sem usuários afetados (affectedUsersQuantity={}, affectedUsers={}). Título: {}",
                    quantidade, listados, input.getAssignment().getTitle());
            throw new InvalidMassiveRequestException(
                    "Quantidade de usuários afetados inválida: informado affectedUsersQuantity=" + quantidade
                            + " e " + listados + " item(ns) em affectedUsers. "
                            + "É necessário ao menos 1 usuário afetado para abrir a massiva. "
                            + "Nenhum protocolo foi aberto no Voalle.");
        }
    }

    private AberturaRegistroMassivoInputDTO decideForIncidentOrMassiveEvent(AberturaRegistroMassivoInputDTO input) {

        log.debug("[MASSIVA] Analisando contratos para identificar presença de B2B...");

        List<AffectedUsersEntity> affectedUsers = input.getAffectedUsers() != null
                ? input.getAffectedUsers()
                : Collections.emptyList();

        boolean hasB2B = employeeService.hasB2BinInput(affectedUsers.stream().map(AffectedUsersEntity::getContractId).toList());
        boolean isMassiveEvent = hasB2B || input.getAffectedUsersQuantity() > 15;
        if(isMassiveEvent){
            log.debug("Critérios atendidos para Evento Massivo (TITLE: {})", input.getAssignment().getTitle());
            input.setIncidentTypeId(MASSIVE_EVENT_INCIDENT_TYPE_ID);
            input.setSolicitationServiceCategory1(MASSIVE_EVENT_CATEGORY_SOCILITATION);
            input.setCatalogServiceId(MASSIVE_EVENT_CATALOG_SERVICE_ID);
            return input;
        }

        input.setIncidentTypeId(NORMAL_EVENT_INCIDENT_TYPE_ID);
        input.setSolicitationServiceCategory1(NORMAL_EVENT_CATEGORY_SOCILITATION);
        input.setCatalogServiceId(NORMAL_EVENT_CATALOG_SERVICE_ID);
        return input;
    }
}