package br.com.sebratel.massivas.service.massivas;

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

@Service
@Slf4j
public class FinalizarMassivaNoEllevenApiService {

    private final WebClient webClient;
    private final RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService;
    private final FinishLinkedProtocolsService finishLinkedProtocolsService;

    @Autowired
    public FinalizarMassivaNoEllevenApiService(WebClient webClient,
                                               RecuperarTokenDoUsuarioIntegradorEllevenService recuperarTokenDoUsuarioIntegradorEllevenService,
                                               FinishLinkedProtocolsService finishLinkedProtocolsService) {
        this.webClient = webClient;
        this.recuperarTokenDoUsuarioIntegradorEllevenService = recuperarTokenDoUsuarioIntegradorEllevenService;
        this.finishLinkedProtocolsService = finishLinkedProtocolsService;
    }

    public FinalizarRegistroMassivoOutputDTO executar(FinalizaRegistroMassivoInputDTO input) {
        log.info("Iniciando processo de finalização de massiva. Verificando protocolos vinculados...");

        // Step 1: Execute FinishLinkedProtocolsService before main finalization
        this.finishLinkedProtocolsService.executar(input);

        log.info("Iniciando finalização de massiva principal no ERP via API {}", this.getClass());
        String token = recuperarTokenDoUsuarioIntegradorEllevenService.executar().accessToken();

        Employee x = JwtInformation.retrieveUserData();
        String email = x.email();
        String name = x.name();
        input.setDescription(input.getDescription() + " - " + name + "("+ email +")");

        return this.webClient
                .mutate()
                .baseUrl("https://erp.sebratel.net.br:45715")
                .build()
                .post()
                .uri("/external/integrations/thirdparty/projects/createsolicitationreport")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(input)
                .retrieve()
                .bodyToMono(FinalizarRegistroMassivoOutputDTO.class)
                .block();
    }
}
