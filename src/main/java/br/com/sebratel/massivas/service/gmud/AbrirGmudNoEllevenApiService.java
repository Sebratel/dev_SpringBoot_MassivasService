package br.com.sebratel.massivas.service.gmud;

import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.model.Employee;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import br.com.sebratel.massivas.utils.CallerIdentity;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Abertura de GMUD (Gestão de Mudança de Rede) no Elleven.
 *
 * <p>Usa a MESMA API do Elleven que a massiva ({@code opendetailedsolicitation}), porém envia a
 * classificação da GMUD <b>exatamente como recebida</b> do frontend — sem a regra de negócio da
 * massiva ({@code AdicionarMassivaNoEllevenApiService.decideForIncidentOrMassiveEvent}), que
 * sobrescreveria {@code incidentTypeId}/{@code catalogServiceId}/categoria para "massiva". Por isso
 * a GMUD precisa deste serviço próprio.</p>
 *
 * <p>Classificação da GMUD (confirmada no Voalle, matrix 6950): incidentTypeId=1084,
 * catalogServiceId=1082, serviceLevelAgreementId=99, matrixType=2, teamCode='2.8',
 * solicitationServiceCategory1='infra - 42'. Esses valores vêm no DTO (o frontend os define).</p>
 */
@Service
@Slf4j
public class AbrirGmudNoEllevenApiService {

    private static final String OPEN_SOLICITATION_URL =
            "https://erp.sebratel.net.br:45715/external/integrations/thirdparty/opendetailedsolicitation";

    private final RecuperarTokenDoUsuarioIntegradorEllevenService tokenService;
    private final WebClient webClient;

    @Autowired
    public AbrirGmudNoEllevenApiService(
            RecuperarTokenDoUsuarioIntegradorEllevenService tokenService,
            WebClient webClient) {
        this.tokenService = tokenService;
        this.webClient = webClient;
    }

    public AberturaRegistroMassivoOutputDTO executar(@Valid AberturaRegistroMassivoInputDTO input) {
        Employee user = CallerIdentity.retrieveUserData();
        String email = user.email();
        String name = user.name();

        // A descrição já vem com a máscara estruturada da GMUD (inclui o solicitante);
        // não anexamos assinatura aqui (diferente da massiva) para não poluir o corpo.

        log.info("[GMUD] Usuário {} ({}) abrindo GMUD: '{}'. [incidentTypeId={}, catalogServiceId={}]",
                name, email, input.getAssignment().getTitle(),
                input.getIncidentTypeId(), input.getCatalogServiceId());

        try {
            String token = tokenService.executar().accessToken();

            long startTime = System.currentTimeMillis();
            AberturaRegistroMassivoOutputDTO response = webClient
                    .post()
                    .uri(OPEN_SOLICITATION_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(AberturaRegistroMassivoOutputDTO.class)
                    .block();
            long duration = System.currentTimeMillis() - startTime;

            if (response == null || !response.isSuccess()) {
                // Elleven respondeu HTTP 200 mas recusou (success=false) — loga o motivo para diagnóstico.
                log.warn("[GMUD-ERRO] Elleven recusou a abertura (success=false) em {}ms. Mensagens: {}. Resposta completa: {}",
                        duration, response != null ? response.getMessages() : null, response);
            } else {
                log.info("[GMUD] Sucesso! GMUD criada no Elleven em {}ms. Resposta: {}", duration, response);
            }
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("[GMUD-ERRO] Erro de autenticação (401) na API Elleven. Invalidando token em cache.");
            tokenService.invalidateToken();
            throw e;
        } catch (WebClientResponseException e) {
            log.error("[GMUD-ERRO] Falha na API Elleven. Status: {}. Response Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("[GMUD-ERRO] Falha crítica ao abrir GMUD. Título: {}. Causa: {}",
                    input.getAssignment().getTitle(), e.getMessage(), e);
            throw e;
        }
    }
}
