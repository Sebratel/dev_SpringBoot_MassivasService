package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.ConfirmacaoEllevenDTO;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaInputDTO;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaOutputDTO;
import br.com.sebratel.massivas.dto.massivas.MassivaCriadaOutputDTO;
import br.com.sebratel.massivas.exceptions.IntegrationEllevenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@Service
public class AdicionarMassivaNoEllevenService {

    private final WebClient webClient;
    @org.springframework.beans.factory.annotation.Value("${elleven.creation-delay:3000}")
    private long creationDelay;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String SEND_EMAIL = "0";
    private static final String SEND_SMS = "0";
    private static final String EMAIL_MODEL_ID = "";
    private static final String RETURN_EMAIL_MODEL_ID = "";
    private static final String SEND_PUSH = "0";
    private static final String PUSH_MODEL_ID = "";
    private static final String RETURN_PUSH_MODEL_ID = "";


    @Autowired
    public AdicionarMassivaNoEllevenService(WebClient webClient) {
        this.webClient = webClient;
    }

    public CriacaoDeMassivaOutputDTO salvarNoBancoERP(CriacaoDeMassivaInputDTO input) {
        log.info("Iniciando processo de criação de massiva no ERP. Data: {}, Hora: {}", input.getStartDate(), input.getStartTime());

        // 1. Criação inicial
        MassivaCriadaOutputDTO massivaCriada = this.criarNovaMassivaNoBanco(input.getStartDate(), input.getStartTime(), input.getCookieString());
        log.info("Massiva criada com id {}", massivaCriada.getId());
        String massivaId = massivaCriada.getId();
        log.info("Massiva criada com sucesso. ID Gerado: {}", massivaId);

        // 2. Pontos de impacto
        log.debug("Configurando pontos de impacto para a massiva ID: {}", massivaId);
        try {
            Thread.sleep(creationDelay);
        } catch (InterruptedException e) {
            log.info("A pausa foi interrompida!");
        }
        ConfirmacaoEllevenDTO confImpacto = this.setarPontosDeImpacto(
                massivaId,
                input.getAccessPointIds(),
                input.getSlotOlt(),
                input.getPortaOlt(),
                input.getAddressListId(),
                input.getCookieString()
        );
        validarResposta(confImpacto, "Adição de pontos de impacto");

        // 3. Conclusão e dados adicionais
        log.debug("Finalizando configuração de dados adicionais e notificações para ID: {}", massivaId);
        ConfirmacaoEllevenDTO confFinal = this.setarLugarDataEDadosAdicionais(massivaId, input);

        validarResposta(confFinal, "Finalização da massiva");

        assert confFinal.getMessage() != null;
        String protocolo = confFinal.getMessage().split("Protocolo: ")[1];
        log.info("Protocolo criado {}", protocolo);
        log.info("Processo de criação de massiva ID: {} concluído com sucesso.", massivaId);

        return CriacaoDeMassivaOutputDTO.builder()
                .id(massivaId).input(input)
                .sendEmail(SEND_EMAIL)
                .sendSms(SEND_SMS)
                .emailModelId(EMAIL_MODEL_ID)
                .returnEmailModelId(RETURN_EMAIL_MODEL_ID)
                .sendPush(SEND_PUSH)
                .pushModelId(PUSH_MODEL_ID)
                .returnPushModelId(RETURN_PUSH_MODEL_ID)
                .protocolo(protocolo)
                .build();

    }

    private void validarResposta(ConfirmacaoEllevenDTO resposta, String etapa) {
        log.info(resposta.toString());
        if (!resposta.isSuccess()) {
            String detalhe = "Success is false";
            log.error("Falha na etapa de integração: {}. Detalhe: {}", etapa, detalhe);
            throw new IntegrationEllevenException(String.format("Erro no Elleven em '%s': %s", etapa, detalhe));
        }
        log.debug("Sucesso em validar a resposta");
    }

    private MassivaCriadaOutputDTO criarNovaMassivaNoBanco(LocalDate data, LocalTime hora, String cookieString) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", "");
        formData.add("start_date", data.format(DATE_FORMATTER));
        formData.add("start_time", hora.format(TIME_FORMATTER));

        return webClient.post()
                .uri("/massive_incidents/createOrUpdateMassive")
                .header(HttpHeaders.COOKIE, cookieString)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleHttpError("criação inicial", response))
                .bodyToMono(MassivaCriadaOutputDTO.class)
                .block();
    }

    private ConfirmacaoEllevenDTO setarPontosDeImpacto(String id, Integer[] apIds, Integer[] slots, Integer[] ports, Integer[] addrs, String cookieString) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("massive_incident_id", id);

        // Logs de debug para rastrear a volumetria dos dados enviados
        log.debug("Mapeando dados: APs={}, Slots={}, Portas={}, Endereços={}",
                apIds != null ? apIds.length : 0,
                slots != null ? slots.length : 0,
                ports != null ? ports.length : 0,
                addrs != null ? addrs.length : 0);

        addListToForm(formData, "access_point_ids[]", apIds);
        addListToForm(formData, "slot_olt[]", slots);
        addListToForm(formData, "port_olt[]", ports);
        addListToForm(formData, "address_list_id[]", addrs);

        ConfirmacaoEllevenDTO response = webClient.post()
                .uri("/massive_incidents/saveMassiveConnections")
                .header(HttpHeaders.COOKIE, cookieString)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.error("Erro na requisição externa ao salvar conexões para o incidente {}. Status: {}",
                            id, clientResponse.statusCode());
                    return handleHttpError("configuração de conexões", clientResponse);
                })
                .bodyToMono(ConfirmacaoEllevenDTO.class)
                .block();

        log.info("Configuração de pontos de impacto concluída com sucesso para o incidente ID: {}", id);

        return response;
    }

    private ConfirmacaoEllevenDTO setarLugarDataEDadosAdicionais(String id, CriacaoDeMassivaInputDTO input) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("company_place_id", String.valueOf(input.getCompanyPlaceId()));
        formData.add("assignment_type_id", String.valueOf(input.getAssignmentTypeId()));
        formData.add("assignment_description", input.getAssignmentDescription());
        formData.add("maintenance_date", input.getMaintenanceDate().format(DATE_FORMATTER));
        formData.add("maintenance_time", input.getMaintenanceTime().format(TIME_FORMATTER));
        formData.add("send_email", SEND_EMAIL);
        formData.add("send_sms", SEND_SMS);
        formData.add("email_model_id", EMAIL_MODEL_ID);
        formData.add("return_email_model_id", RETURN_EMAIL_MODEL_ID);
        formData.add("send_push", SEND_PUSH);
        formData.add("push_model_id", PUSH_MODEL_ID);
        formData.add("return_push_model_id", RETURN_PUSH_MODEL_ID);
        addListToForm(formData, "access_point_ids[]", input.getAccessPointIds());

        log.info(formData.toString());
        String uri = "/massive_incidents/concludeMassiveIncident/" + id;
        log.info("Realizado post para {}", uri);
        return webClient.post()
                .uri(uri)
                .header(HttpHeaders.COOKIE, input.getCookieString())
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleHttpError("conclusão", response))
                .bodyToMono(ConfirmacaoEllevenDTO.class)
                .block();
    }

    private void addListToForm(MultiValueMap<String, String> form, String key, Integer[] values) {
        if (values != null) {
            Arrays.stream(values).forEach(v -> form.add(key, String.valueOf(v)));
        }
    }

    private Mono<? extends Throwable> handleHttpError(String context, ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.error("Erro HTTP na etapa {}: Status {} - Body: {}", context, response.statusCode(), body);
                    return Mono.error(new RuntimeException("Falha na integração Elleven: " + body));
                });
    }


}