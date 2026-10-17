package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.massivas.MassivaHistoryEncerramentoInputDTO;
import br.com.sebratel.massivas.dto.massivas.MassivaHistoryOutputDTO;
import br.com.sebratel.massivas.service.massivas.EncerrarMassivaHistoryService;
import br.com.sebratel.massivas.service.massivas.RecuperarTodasAsMassivasHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/massivas-history", "/api/v1/massivas-splitters"})
@Slf4j
public class MassivaHistoryController {

    private final RecuperarTodasAsMassivasHistoryService recuperarTodasAsMassivasHistoryService;
    private final EncerrarMassivaHistoryService encerrarMassivaHistoryService;

    @Autowired
    public MassivaHistoryController(RecuperarTodasAsMassivasHistoryService recuperarTodasAsMassivasHistoryService,
                                    EncerrarMassivaHistoryService encerrarMassivaHistoryService) {
        this.recuperarTodasAsMassivasHistoryService = recuperarTodasAsMassivasHistoryService;
        this.encerrarMassivaHistoryService = encerrarMassivaHistoryService;
    }

    @GetMapping({"/recover-via-database", "/recuperar-pelo-banco"})
    public ResponseEntity<ApiResponse<List<MassivaHistoryOutputDTO>>> retrieveAllMassivaHistoryViaDatabase() {
        try {
            List<MassivaHistoryOutputDTO> output = recuperarTodasAsMassivasHistoryService.executar();

            log.info("Massiva history successfully retrieved from splitters database.");

            ApiResponse<List<MassivaHistoryOutputDTO>> response = ApiResponse.<List<MassivaHistoryOutputDTO>>builder()
                    .success(true)
                    .message("Massiva history successfully retrieved.")
                    .data(output)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error retrieving massiva history from splitters database. {}", e.getMessage());
            throw e;
        }
    }

    @PatchMapping({"/{protocol}/finalize", "/{protocol}/encerrar"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeMassivaHistory(
            @PathVariable("protocol") Long protocol,
            @RequestBody(required = false) MassivaHistoryEncerramentoInputDTO input) {
        try {
            int atualizados = encerrarMassivaHistoryService.encerrar(protocol, input);

            String message = atualizados > 0
                    ? "Massiva history finalized on splitters side."
                    : "No open massiva history found for this protocol (already closed or nonexistent).";

            log.info("Finalize massiva history [protocol={}]: {} row(s) updated.", protocol, atualizados);

            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message(message)
                    .data(Map.of("protocol", protocol, "atualizados", atualizados))
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error finalizing massiva history on splitters side [protocol={}]. {}", protocol, e.getMessage());
            throw e;
        }
    }
}
