package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.AberturaProtocoloInfraInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoAssignmentDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.enums.InfraProtocolType;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;

/**
 * Abre um protocolo de infraestrutura no ERP Voalle usando o mesmo endpoint da massiva
 * ({@code opendetailedsolicitation}), porém carimbando as constantes do tipo de infra escolhido.
 * Diferente da massiva, NÃO anexa o operador na descrição (a máscara do frontend já cobre) e
 * NÃO aplica regra de negócio para trocar o tipo (o operador escolhe o tipo).
 */
@Service
@Slf4j
public class AbrirProtocoloInfraNoEllevenApiService {

    // Constantes compartilhadas por todos os tipos de protocolo de infraestrutura.
    private static final int INCIDENT_STATUS_ABERTURA = 1;
    private static final int MATRIX_TYPE_INTERNA = 2;
    private static final int SERVICE_LEVEL_AGREEMENT_ID = 99;
    private static final int COMPANY_PLACE_ID = 6;
    private static final String TEAM_CODE = "1093";
    // Categorias enviadas como "etiqueta" (tag) da categoria no Voalle — NÃO é o id nem o nome.
    // Sem a etiqueta correta o ERP responde code 1000 "Nenhuma etiqueta foi encontrada para consulta das categorias".
    private static final String CATEGORY_1_INFRAESTRUTURA = "0a9e9b70";
    private static final String CATEGORY_2_MANUTENCAO_FO = "1f77f32c";

    private static final String OPEN_DETAILED_SOLICITATION_URL =
            "https://erp.sebratel.net.br:45715/external/integrations/thirdparty/opendetailedsolicitation";

    private final RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService;
    private final WebClient webClient;

    @Autowired
    public AbrirProtocoloInfraNoEllevenApiService(RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService,
                                                  WebClient webClient) {
        this.recuperarTokenDoUsuarioIntegradorEllevenService = recuperarTokenDoUsuarioIntegradorEllevenService;
        this.webClient = webClient;
    }

    public AberturaRegistroMassivoOutputDTO executar(@Valid AberturaProtocoloInfraInputDTO input) {
        InfraProtocolType type = InfraProtocolType.fromCode(input.getInfraType());

        log.info("[INFRA] Solicitando abertura de protocolo de infraestrutura. Tipo: {}, PersonId: {}, AP: {}",
                type.getCode(), input.getPersonId(), input.getAuthenticationAccessPointCode());

        AberturaRegistroMassivoInputDTO body = buildVoalleBody(input, type);

        try {
            log.debug("[INFRA] Buscando token de integracao...");
            String token = recuperarTokenDoUsuarioIntegradorEllevenService.executar().accessToken();

            log.info("[INFRA] Enviando payload para Elleven API: {} (tipo {})", OPEN_DETAILED_SOLICITATION_URL, type.getCode());

            long startTime = System.currentTimeMillis();

            AberturaRegistroMassivoOutputDTO response = webClient
                    .post()
                    .uri(OPEN_DETAILED_SOLICITATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AberturaRegistroMassivoOutputDTO.class)
                    .block();

            long duration = System.currentTimeMillis() - startTime;

            log.info("[INFRA] Sucesso! Protocolo de infraestrutura criado no Elleven em {}ms. Resposta: {}", duration, response);
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("[INFRA-ERRO] Erro de autenticacao (401) na API Elleven. Invalidando token em cache.");
            recuperarTokenDoUsuarioIntegradorEllevenService.invalidateToken();
            throw e;
        } catch (WebClientResponseException e) {
            log.error("[INFRA-ERRO] Falha na API Elleven. Status: {}. Response Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[INFRA-ERRO] Falha critica ao abrir protocolo de infraestrutura. Tipo: {}. Causa: {}",
                    input.getInfraType(), e.getMessage(), e);
            throw e;
        }
    }

    private AberturaRegistroMassivoInputDTO buildVoalleBody(AberturaProtocoloInfraInputDTO input, InfraProtocolType type) {
        AberturaRegistroMassivoAssignmentDTO assignment = input.getAssignment();
        // Local de atendimento é fixo para infraestrutura (independe do que vier do frontend).
        assignment.setCompanyPlaceId(COMPANY_PLACE_ID);

        AberturaRegistroMassivoInputDTO body = new AberturaRegistroMassivoInputDTO();
        body.setIncidentStatusId(INCIDENT_STATUS_ABERTURA);
        body.setPersonId(input.getPersonId());
        body.setIncidentTypeId(type.getIncidentTypeId());
        body.setCatalogServiceId(type.getCatalogServiceId());
        body.setServiceLevelAgreementId(SERVICE_LEVEL_AGREEMENT_ID);
        body.setMatrixType(MATRIX_TYPE_INTERNA);
        body.setTeamCode(TEAM_CODE);
        body.setSolicitationServiceCategory1(CATEGORY_1_INFRAESTRUTURA);
        body.setSolicitationServiceCategory2(CATEGORY_2_MANUTENCAO_FO);
        body.setSolicitationServiceCategory3(type.getCategory3());
        body.setAuthenticationAccessPointCode(input.getAuthenticationAccessPointCode());
        // Site: obrigatório para backbone; nos demais tipos vem null e o @JsonInclude(NON_NULL) omite.
        body.setAuthenticationSiteCode(input.getAuthenticationSiteCode());
        body.setAssignment(assignment);
        body.setAffectedUsers(new ArrayList<>());
        return body;
    }
}
