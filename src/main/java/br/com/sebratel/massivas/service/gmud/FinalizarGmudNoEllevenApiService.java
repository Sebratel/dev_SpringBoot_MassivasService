package br.com.sebratel.massivas.service.gmud;

import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.model.Employee;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import br.com.sebratel.massivas.utils.JwtInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Encerramento de GMUD (Gestão de Mudança de Rede) no Elleven.
 *
 * <p>Usa a MESMA API do Elleven que a finalização da massiva
 * ({@code projects/createsolicitationreport}), porém <b>sem</b> a etapa de
 * {@code FinishLinkedProtocolsService} (que é específica da massiva — procura e encerra os
 * protocolos de infraestrutura vinculados). A GMUD encerra apenas o seu próprio atendimento
 * ({@code assignmentId}).</p>
 *
 * <p>É disparado quando a GMUD é marcada como <b>Concluída</b> (encerramento,
 * {@code incidentStatusId} de encerrado) ou <b>Negada</b> pelo Comitê (cancelamento,
 * {@code incidentStatusId} = 8). O status e a descrição vêm no DTO, montados pelo frontend.</p>
 */
@Service
@Slf4j
public class FinalizarGmudNoEllevenApiService {

    private static final String FINALIZE_SOLICITATION_URL =
            "https://erp.sebratel.net.br:45715/external/integrations/thirdparty/projects/createsolicitationreport";

    private final WebClient webClient;
    private final RecuperarTokenDoUsuarioIntegradorEllevenService tokenService;

    @Autowired
    public FinalizarGmudNoEllevenApiService(
            WebClient webClient,
            RecuperarTokenDoUsuarioIntegradorEllevenService tokenService) {
        this.webClient = webClient;
        this.tokenService = tokenService;
    }

    public FinalizarRegistroMassivoOutputDTO executar(FinalizaRegistroMassivoInputDTO input) {
        Employee user = JwtInformation.retrieveUserData();
        String email = user.email();
        String name = user.name();

        // A descrição já vem com o texto do encerramento (inclui solicitante/data) montado no
        // frontend; não anexamos assinatura aqui (diferente da massiva) para não poluir o corpo.
        log.info("[GMUD-ENCERRAR] Usuário {} ({}) encerrando GMUD. [assignmentId={}, incidentStatusId={}]",
                name, email, input.getAssignmentId(), input.getIncidentStatusId());

        try {
            String token = tokenService.executar().accessToken();

            long startTime = System.currentTimeMillis();
            FinalizarRegistroMassivoOutputDTO response = webClient
                    .mutate()
                    .baseUrl("https://erp.sebratel.net.br:45715")
                    .build()
                    .post()
                    .uri(FINALIZE_SOLICITATION_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(FinalizarRegistroMassivoOutputDTO.class)
                    .block();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[GMUD-ENCERRAR] Sucesso! GMUD encerrada no Elleven em {}ms. Resposta: {}", duration, response);
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("[GMUD-ENCERRAR-ERRO] Erro de autenticação (401) na API Elleven. Invalidando token em cache.");
            tokenService.invalidateToken();
            throw e;
        } catch (WebClientResponseException e) {
            log.error("[GMUD-ENCERRAR-ERRO] Falha na API Elleven. Status: {}. Response Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GMUD-ENCERRAR-ERRO] Falha crítica ao encerrar GMUD. assignmentId: {}. Causa: {}",
                    input.getAssignmentId(), e.getMessage(), e);
            throw e;
        }
    }
}
