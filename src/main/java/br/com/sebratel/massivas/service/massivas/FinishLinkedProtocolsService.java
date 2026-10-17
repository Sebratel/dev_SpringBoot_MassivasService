package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.repository.erp.massivas.FinishLinkedProtocolsRepository;
import br.com.sebratel.massivas.repository.erp.projections.FinishLinkedProtocolsProjection;
import br.com.sebratel.massivas.service.RecuperarTokenDoUsuarioIntegradorEllevenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@Slf4j
public class FinishLinkedProtocolsService {

    private final FinishLinkedProtocolsRepository repository;
    private final WebClient webClient;
    private final RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService;

    @Autowired
    public FinishLinkedProtocolsService(FinishLinkedProtocolsRepository repository,
                                       WebClient webClient,
                                       RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService) {
        this.repository = repository;
        this.webClient = webClient;
        this.recuperarTokenDoUsuarioIntegradorEllevenService = recuperarTokenDoUsuarioIntegradorEllevenService;
    }

    public void executar(FinalizaRegistroMassivoInputDTO mainInput) {
        log.info("Checking for linked protocols to finalize. Main Assignment ID: {}", mainInput.getAssignmentId());

        List<FinishLinkedProtocolsProjection> linkedProtocols = repository.findLinkedProtocols(mainInput.getAssignmentId());
        log.info("Found {} linked protocols to finalize.", linkedProtocols.size());

        String token = recuperarTokenDoUsuarioIntegradorEllevenService.executar().accessToken();

        for (FinishLinkedProtocolsProjection linked : linkedProtocols) {
            // linked.getASSIGNMENT_LINKADO() could be null if it's a LEFT JOIN and no link exists,
            // but the query logic should generally return valid links.
            if (linked.getASSIGNMENT_LINKADO() == null) continue;

            log.info("Finalizing linked protocol: {} (Assignment ID: {})", linked.getPROTOLOCO_LINKADO(), linked.getASSIGNMENT_LINKADO());

            FinalizaRegistroMassivoInputDTO linkedInput = FinalizaRegistroMassivoInputDTO.builder()
                    .assignmentId(linked.getASSIGNMENT_LINKADO().toString())
                    .incidentStatusId(mainInput.getIncidentStatusId())
                    .description(mainInput.getDescription())
                    .progress(mainInput.getProgress())
                    .priority(mainInput.getPriority())
                    .notificationTarget(mainInput.getNotificationTarget())
                    .privateReport(mainInput.getPrivateReport())
                    .build();

            try {
                FinalizarRegistroMassivoOutputDTO response = this.webClient
                        .mutate()
                        .baseUrl("https://erp.sebratel.net.br:45715")
                        .build()
                        .post()
                        .uri("/external/integrations/thirdparty/projects/createsolicitationreport")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .bodyValue(linkedInput)
                        .retrieve()
                        .bodyToMono(FinalizarRegistroMassivoOutputDTO.class)
                        .block();

                if (response == null || !response.isSuccess()) {
                    String errorMessage = "Unknown error";
                    if (response != null && response.getMessages() != null && !response.getMessages().isEmpty()) {
                        errorMessage = response.getMessages().get(0).getMessage();
                    }
                    log.error("Failed to finalize linked protocol {}. Error: {}", linked.getPROTOLOCO_LINKADO(), errorMessage);
                    throw new RuntimeException("Failed to finalize linked protocol: " + linked.getPROTOLOCO_LINKADO() + ". " + errorMessage);
                }

                log.info("Successfully finalized linked protocol: {}", linked.getPROTOLOCO_LINKADO());
            } catch (Exception e) {
                log.error("Error finalizing linked protocol {}: {}", linked.getPROTOLOCO_LINKADO(), e.getMessage());
                throw new RuntimeException("Error finalizing linked protocol: " + linked.getPROTOLOCO_LINKADO(), e);
            }
        }
    }
}
