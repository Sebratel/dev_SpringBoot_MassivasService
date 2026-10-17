package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.EllevenCompleteTaskResponseDTO;
import br.com.sebratel.massivas.service.gmud.AbrirGmudNoEllevenApiService;
import br.com.sebratel.massivas.service.gmud.FinalizarGmudNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.MassivaIdempotencyStore;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Abertura de GMUD (Gestão de Mudança de Rede) no Elleven. Endpoint próprio (separado da massiva)
 * porque a classificação não pode ser sobrescrita pela regra da massiva. Reusa a idempotência
 * ({@link MassivaIdempotencyStore}) — retry após timeout não duplica o protocolo.
 */
@RestController
@RequestMapping("/api/v1/gmud")
@Slf4j
public class GmudEllevenController {

    private final AbrirGmudNoEllevenApiService abrirGmudNoEllevenApiService;
    private final FinalizarGmudNoEllevenApiService finalizarGmudNoEllevenApiService;
    private final MassivaIdempotencyStore idempotencyStore;

    @Autowired
    public GmudEllevenController(
            AbrirGmudNoEllevenApiService abrirGmudNoEllevenApiService,
            FinalizarGmudNoEllevenApiService finalizarGmudNoEllevenApiService,
            MassivaIdempotencyStore idempotencyStore) {
        this.abrirGmudNoEllevenApiService = abrirGmudNoEllevenApiService;
        this.finalizarGmudNoEllevenApiService = finalizarGmudNoEllevenApiService;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping({"/abrir-via-api", "open-via-api"})
    public ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> abrirGmudViaApi(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AberturaRegistroMassivoInputDTO input) {
        return idempotencyStore.execute(idempotencyKey, () -> doAbrirGmudViaApi(input));
    }

    private ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> doAbrirGmudViaApi(
            AberturaRegistroMassivoInputDTO input) {

        log.info("Starting GMUD creation in ERP via API. [Requester: {}]", input.getPersonId());

        try {
            AberturaRegistroMassivoOutputDTO output = abrirGmudNoEllevenApiService.executar(input);

            if (!output.isSuccess()) {
                ApiResponse<AberturaRegistroMassivoOutputDTO> apiResponse =
                        ApiResponse.<AberturaRegistroMassivoOutputDTO>builder()
                                .success(false)
                                .message("Failed to create GMUD in ERP. Elleven side ERROR")
                                .data(output)
                                .build();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(apiResponse);
            }

            log.info("GMUD successfully created in ERP. [PROTOCOL ID: {}, ASSIGNMENT ID: {}]",
                    output.getResponse().getProtocol(),
                    output.getResponse().getAssignmentId());

            ApiResponse<AberturaRegistroMassivoOutputDTO> response =
                    ApiResponse.<AberturaRegistroMassivoOutputDTO>builder()
                            .success(true)
                            .message("GMUD successfully created in ERP.")
                            .data(output)
                            .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating GMUD in ERP for requester {}: {}", input.getPersonId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Encerra o protocolo da GMUD no Elleven. Disparado quando a GMUD é marcada como Concluída
     * (encerramento) ou Negada (cancelamento, incidentStatusId=8). NÃO usa a lógica de protocolos
     * vinculados da massiva — encerra só o próprio {@code assignmentId}.
     */
    @DeleteMapping({"/encerrar-via-api", "finalizar-via-api"})
    public ResponseEntity<FinalizarRegistroMassivoOutputDTO> encerrarGmudViaApi(
            @Valid @RequestBody FinalizaRegistroMassivoInputDTO input) {
        return registrarNoElleven(input, "finalization");
    }

    /**
     * Registra um relato no protocolo da GMUD sem encerrá-lo (ex.: reagendamento da janela).
     * Mesma API do encerramento ({@code createsolicitationreport}); o frontend envia o
     * {@code incidentStatusId} ATUAL do protocolo, então o status não muda.
     */
    @PostMapping({"/relato-via-api", "report-via-api"})
    public ResponseEntity<FinalizarRegistroMassivoOutputDTO> relatoGmudViaApi(
            @Valid @RequestBody FinalizaRegistroMassivoInputDTO input) {
        if (input.getIncidentStatusId() == null || input.getIncidentStatusId().isBlank()) {
            FinalizarRegistroMassivoOutputDTO badRequest = FinalizarRegistroMassivoOutputDTO.builder()
                    .success(false)
                    .messages(List.of(EllevenCompleteTaskResponseDTO.builder()
                            .message("incidentStatusId (status atual do protocolo) é obrigatório no relato.")
                            .type("Error")
                            .build()))
                    .build();
            return ResponseEntity.badRequest().body(badRequest);
        }
        return registrarNoElleven(input, "report");
    }

    private ResponseEntity<FinalizarRegistroMassivoOutputDTO> registrarNoElleven(
            FinalizaRegistroMassivoInputDTO input, String acao) {

        log.info("Starting GMUD {} in ERP via API. [ASSIGNMENT ID: {}]", acao, input.getAssignmentId());

        try {
            FinalizarRegistroMassivoOutputDTO output = finalizarGmudNoEllevenApiService.executar(input);

            if (output != null && output.isSuccess()) {
                log.info("GMUD {} successfully registered in ERP. [ASSIGNMENT ID: {}]", acao, input.getAssignmentId());
                return ResponseEntity.ok(output);
            }

            log.warn("Error on GMUD {} in ERP. Elleven side ERROR. [ASSIGNMENT ID: {}]", acao, input.getAssignmentId());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(output);
        } catch (Exception e) {
            log.error("Error on GMUD {} SERVER ERROR. ASSIGNMENT: {}: {}", acao, input.getAssignmentId(), e.getMessage());

            FinalizarRegistroMassivoOutputDTO errorOutput = FinalizarRegistroMassivoOutputDTO.builder()
                    .success(false)
                    .messages(List.of(EllevenCompleteTaskResponseDTO.builder()
                            .message("Internal server error on GMUD " + acao + ": " + e.getMessage())
                            .type("Error")
                            .build()))
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorOutput);
        }
    }
}
