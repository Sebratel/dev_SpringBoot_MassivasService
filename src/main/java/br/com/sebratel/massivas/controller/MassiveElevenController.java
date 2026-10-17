package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.massivas.api.EllevenCompleteTaskResponseDTO;

import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaInputDTO;
import br.com.sebratel.massivas.dto.massivas.CriacaoDeMassivaOutputDTO;
import br.com.sebratel.massivas.dto.massivas.EllevenApiResponseDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.dto.massivas.MassivasBFFOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaProtocoloInfraInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.AberturaRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizarRegistroMassivoOutputDTO;
import br.com.sebratel.massivas.service.massivas.AbrirProtocoloInfraNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.FinalizarMassivaNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.RecuperarPrevisaoMassivaPorContratoService;
import br.com.sebratel.massivas.service.massivas.RecuperarTodasAsMassivasPeloBancoService;
import br.com.sebratel.massivas.service.massivas.AdicionarMassivaNoEllevenApiService;
import br.com.sebratel.massivas.service.massivas.AdicionarMassivaNoEllevenService;
import br.com.sebratel.massivas.service.massivas.EnviarListaDeAfetadosParaNativeService;
import br.com.sebratel.massivas.service.massivas.GetAllMassivesService;
import br.com.sebratel.massivas.service.massivas.MassivaIdempotencyStore;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/massive-incidents", "/api/v1/massivas-eleven", "/api/v1/massivas"})
@Slf4j
public class MassiveElevenController {

    private final AdicionarMassivaNoEllevenService adicionarMassivaNoEllevenService;
    private final AdicionarMassivaNoEllevenApiService adicionarMassivaNoEllevenApiService;
    private final GetAllMassivesService getAllMassivesService;
    private final RecuperarTodasAsMassivasPeloBancoService recuperarTodasAsMassivasPeloBancoService;
    private final RecuperarPrevisaoMassivaPorContratoService recuperarPrevisaoMassivaPorContratoService;
    private final FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService;
    private final AbrirProtocoloInfraNoEllevenApiService abrirProtocoloInfraNoEllevenApiService;
    private final MassivaIdempotencyStore idempotencyStore;

    @Autowired
    public MassiveElevenController(AdicionarMassivaNoEllevenService adicionarMassivaNoEllevenService, AdicionarMassivaNoEllevenApiService adicionarMassivaNoEllevenApiService,
                                    EnviarListaDeAfetadosParaNativeService enviarListaDeAfetadosParaNativeService,
                                    GetAllMassivesService getAllMassivesService, RecuperarTodasAsMassivasPeloBancoService recuperarTodasAsMassivasPeloBancoService, RecuperarPrevisaoMassivaPorContratoService recuperarPrevisaoMassivaPorContratoService, FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService,
                                    AbrirProtocoloInfraNoEllevenApiService abrirProtocoloInfraNoEllevenApiService,
                                    MassivaIdempotencyStore idempotencyStore) {
        this.adicionarMassivaNoEllevenService = adicionarMassivaNoEllevenService;
        this.adicionarMassivaNoEllevenApiService = adicionarMassivaNoEllevenApiService;
        this.getAllMassivesService = getAllMassivesService;
        this.recuperarTodasAsMassivasPeloBancoService = recuperarTodasAsMassivasPeloBancoService;
        this.recuperarPrevisaoMassivaPorContratoService = recuperarPrevisaoMassivaPorContratoService;
        this.finalizarMassivaNoEllevenApiService = finalizarMassivaNoEllevenApiService;
        this.abrirProtocoloInfraNoEllevenApiService = abrirProtocoloInfraNoEllevenApiService;
        this.idempotencyStore = idempotencyStore;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CriacaoDeMassivaOutputDTO>> createMassiveIncidentWithFlutterData(
            @Valid @RequestBody CriacaoDeMassivaInputDTO input) {

        log.info("Starting massive incident creation in ERP. [Requester: {}]", input.getAssignmentDescription());

        try {
            CriacaoDeMassivaOutputDTO output = adicionarMassivaNoEllevenService.salvarNoBancoERP(input);

            log.info("Massive incident successfully created in ERP. [Massive ID: {}]", output.getId());

            ApiResponse<CriacaoDeMassivaOutputDTO> response = ApiResponse.<CriacaoDeMassivaOutputDTO>builder()
                    .success(true)
                    .message("Massive incident successfully created in ERP.")
                    .data(output)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating massive incident in ERP for requester {}: {}", input.getAssignmentDescription(), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/getAllMassives")
    public ResponseEntity<ApiResponse<EllevenApiResponseDTO>> retrieveAllMassiveIncidents() {

        log.debug("Requesting retrieval of all massive incidents from Elleven system.");

        EllevenApiResponseDTO output = getAllMassivesService.getAllSolicitations();

        int totalRetrieved = (output.getResponse() != null) ? output.getResponse().getTotalRecords() : 0;
        log.info("Massive incident retrieval finished. [Total found: {}]", totalRetrieved);

        ApiResponse<EllevenApiResponseDTO> response = ApiResponse.<EllevenApiResponseDTO>builder()
                .success(true)
                .message("Massive incidents successfully retrieved")
                .data(output)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping({"/recover-via-database", "/recuperar-pelo-banco"})
    public ResponseEntity<ApiResponse<List<MassivasBFFOutputDTO>>> retrieveAllMassiveIncidentsViaDatabase() {
        try {
            List<MassivasBFFOutputDTO> output = recuperarTodasAsMassivasPeloBancoService.executar();

            log.info("Massive incidents successfully retrieved from ERP database.");

            ApiResponse<List<MassivasBFFOutputDTO>> response = ApiResponse.<List<MassivasBFFOutputDTO>>builder()
                    .success(true)
                    .message("Massive incidents successfully retrieved.")
                    .data(output)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error retrieving massive incidents from ERP database. {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping({"/estimated-end/contract/{contractNumber}", "/previsao-finalizacao/contrato/{contractNumber}"})
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> retrieveEstimatedEndByContract(@PathVariable String contractNumber) {
        log.info("Retrieving estimated end of massive incident from ERP database. [Contract: {}]", contractNumber);

        ImpactedUsersOutputDTO output = recuperarPrevisaoMassivaPorContratoService.executar(contractNumber);

        log.info("Estimated end successfully retrieved for contract {}.", contractNumber);

        ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                .success(true)
                .message("Estimated end of massive incident successfully retrieved.")
                .data(output)
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping({"/save-massive-via-api", "salvar-massiva-via-api"})
    public ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> createMassiveIncidentViaApi(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AberturaRegistroMassivoInputDTO input) {
        return idempotencyStore.execute(idempotencyKey, () -> doCreateMassiveIncidentViaApi(input));
    }

    private ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> doCreateMassiveIncidentViaApi(
            AberturaRegistroMassivoInputDTO input) {

        log.info("Starting massive incident creation in ERP via API. [Requester: {}]", input.getPersonId());

        try {
            AberturaRegistroMassivoOutputDTO output = adicionarMassivaNoEllevenApiService.executar(input);

            if(!output.isSuccess()) {
                ApiResponse<AberturaRegistroMassivoOutputDTO> apiResponse = ApiResponse.<AberturaRegistroMassivoOutputDTO>builder().success(false).message("Failed to create massive incident in ERP. Elleven side ERROR").data(output).build();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(apiResponse);
            }

            log.info("Massive incident successfully created in ERP. [PROTOCOL ID: {}, ASSIGNMENT ID: {}, MESSAGE: {}]",
                    output.getResponse().getProtocol(),
                    output.getResponse().getAssignmentId(),
                    output.getResponse().getMessage());


            ApiResponse<AberturaRegistroMassivoOutputDTO> response = ApiResponse.<AberturaRegistroMassivoOutputDTO>builder()
                    .success(true)
                    .message("Massive incident successfully created in ERP.")
                    .data(output)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating massive incident in ERP for requester {}: {}", input.getPersonId(), e.getMessage());
            throw e;
        }
    }

    @PostMapping({"/open-infra-solicitation-via-api", "abrir-protocolo-infra-via-api"})
    public ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> openInfraSolicitationViaApi(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AberturaProtocoloInfraInputDTO input) {
        return idempotencyStore.execute(idempotencyKey, () -> doOpenInfraSolicitationViaApi(input));
    }

    private ResponseEntity<ApiResponse<AberturaRegistroMassivoOutputDTO>> doOpenInfraSolicitationViaApi(
            AberturaProtocoloInfraInputDTO input) {

        log.info("Starting infrastructure protocol creation in ERP via API. [Type: {}, Requester: {}]",
                input.getInfraType(), input.getPersonId());

        try {
            AberturaRegistroMassivoOutputDTO output = abrirProtocoloInfraNoEllevenApiService.executar(input);

            if (!output.isSuccess()) {
                ApiResponse<AberturaRegistroMassivoOutputDTO> apiResponse = ApiResponse.<AberturaRegistroMassivoOutputDTO>builder()
                        .success(false)
                        .message("Failed to create infrastructure protocol in ERP. Elleven side ERROR")
                        .data(output)
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(apiResponse);
            }

            log.info("Infrastructure protocol successfully created in ERP. [PROTOCOL ID: {}, ASSIGNMENT ID: {}, MESSAGE: {}]",
                    output.getResponse().getProtocol(),
                    output.getResponse().getAssignmentId(),
                    output.getResponse().getMessage());

            ApiResponse<AberturaRegistroMassivoOutputDTO> response = ApiResponse.<AberturaRegistroMassivoOutputDTO>builder()
                    .success(true)
                    .message("Infrastructure protocol successfully created in ERP.")
                    .data(output)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating infrastructure protocol in ERP for requester {}: {}", input.getPersonId(), e.getMessage());
            throw e;
        }
    }

    @DeleteMapping({"/finalize-ticket-via-api", "finalizar-chamado-via-api"})
    public ResponseEntity<FinalizarRegistroMassivoOutputDTO> finalizeMassiveIncidentViaApi(
            @Valid @RequestBody FinalizaRegistroMassivoInputDTO input) {

        log.info("Starting massive incident finalization in ERP via API.");

        try {
            FinalizarRegistroMassivoOutputDTO output = finalizarMassivaNoEllevenApiService.executar(input);

            if (output.isSuccess()) {
                log.info("Massive incident successfully finalized in ERP. [ASSIGNMENT ID: {}, MESSAGE: {}]",
                        input.getAssignmentId(),
                        input.getDescription());

                return ResponseEntity.ok(output);

            } else {
                log.warn("Error finalizing massive incident in ERP. [ASSIGNMENT ID: {}, MESSAGE: {}]",
                        input.getAssignmentId(),
                        input.getDescription());

                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(output);
            }
        } catch (Exception e) {
            log.error("Error finalizing massive incident SERVER ERROR. ASSIGNMENT: {}: {}", input.getAssignmentId(), e.getMessage());

            FinalizarRegistroMassivoOutputDTO errorOutput = FinalizarRegistroMassivoOutputDTO.builder()
                    .success(false)
                    .messages(List.of(EllevenCompleteTaskResponseDTO.builder()
                            .message("Internal server error while finalizing massive incident: " + e.getMessage())
                            .type("Error")
                            .build()))
                    .build();

            // If it's a RuntimeException thrown by our service due to external API failure, it should be 502
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Failed to finalize linked protocol") || msg.contains("Error finalizing linked protocol"))) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorOutput);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorOutput);
        }
    }

}
