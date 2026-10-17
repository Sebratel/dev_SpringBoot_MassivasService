package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.CreateImpactedUsersInputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.service.AffectedUserService;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import br.com.sebratel.massivas.utils.DatabaseErrorParser;


@Slf4j
@RestController
@RequestMapping({"/api/v1/impacted-users", "/api/v1/usuario-afetado", "/api/v1/afetados"})
public class AffectedUserController {

    private final AffectedUserService affectedUSerService;

    @Autowired
    public AffectedUserController(AffectedUserService affectedUSerService) {
        this.affectedUSerService = affectedUSerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> getAllImpactedUsers() {
        log.info("Starting search for impacted users");
        try {
            ImpactedUsersOutputDTO impactedUsers = affectedUSerService.getAll();

            if (impactedUsers.getImpactedUsers().isEmpty()) {
                log.warn("No impacted users found.");
                ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                        .success(false)
                        .message("No impacted users found")
                        .data(impactedUsers)
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("Search completed. Found {} users.", impactedUsers.getImpactedUsers().size());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Impacted users successfully found.")
                    .data(impactedUsers)
                    .build();
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.info("Could not find users: {}", e.getMessage());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error searching impacted users: {}", e.getMessage(), e);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> createImpactedUser(@Valid @RequestBody CreateImpactedUsersInputDTO input) {
        log.info("Starting creation process of impacted user for protocol");
        try {
            ImpactedUsersOutputDTO savedUser = affectedUSerService.createImpactedUsersDTO(input);
            log.info("Impacted users for protocol successfully created.");

            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Impacted user successfully created.")
                    .data(savedUser)
                    .build();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating impacted users for protocol: {}", e.getMessage());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Error creating impacted user: " + e.getMessage())
                    .errors(DatabaseErrorParser.parse(e.getMessage()))
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/pppoe/{pppoe}")
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> getImpactedUsersByPppoe(@PathVariable String pppoe) {
        log.info("Starting search for impacted users by PPPoE: {}", pppoe);
        try {
            ImpactedUsersOutputDTO impactedUsers = affectedUSerService.getUsuariosAfetadosByPppoe(pppoe);

            if (impactedUsers.getImpactedUsers().isEmpty()) {
                log.warn("No impacted user found for PPPoE: {}", pppoe);
            } else {
                log.info("Search by PPPoE {} completed. Found {} users.", pppoe, impactedUsers.getImpactedUsers().size());
            }

            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Search by PPPoE successfully performed.")
                    .data(impactedUsers)
                    .build();
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.info("Could not find users by PPPoE {}: {}", pppoe, e.getMessage());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by PPPoE: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error searching users by PPPoE {}: {}", pppoe, e.getMessage(), e);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by PPPoE: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> getImpactedUsersByContractId(@PathVariable Long contractId) {
        log.info("Starting search for impacted users by contractId: {}", contractId);
        try {
            ImpactedUsersOutputDTO impactedUsers = affectedUSerService.getUsuariosAfetadosByContractId(contractId);

            if (impactedUsers.getImpactedUsers().isEmpty()) {
                log.warn("No impacted user found for contractId: {}", contractId);
            } else {
                log.info("Search by contractId {} completed. Found {} users.", contractId, impactedUsers.getImpactedUsers().size());
            }

            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Search by PPPoE successfully performed.")
                    .data(impactedUsers)
                    .build();
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.info("Could not find users by contractId {}: {}", contractId, e.getMessage());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by contractId: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error searching users by contractId {}: {}", contractId, e.getMessage(), e);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by contractId: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }


    @GetMapping("/protocol/{protocol}")
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> getImpactedUsersByProtocol(@PathVariable Long protocol) {
        log.info("Starting search for impacted users by protocol: {}", protocol);
        try {
            ImpactedUsersOutputDTO impactedUsers = affectedUSerService.getUsuariosByProtocol(protocol);

            if (impactedUsers.getImpactedUsers().isEmpty()) {
                log.warn("No impacted user found for protocol: {}", protocol);
                ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                        .success(false)
                        .message("No impacted user found for protocol: " + protocol)
                        .data(impactedUsers)
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("Search by protocol {} completed. Found {} users.", protocol, impactedUsers.getImpactedUsers().size());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Impacted users successfully found.")
                    .data(impactedUsers)
                    .build();
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.info("Could not find users by protocol {}: {}", protocol, e.getMessage());
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by protocol: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error searching users by protocol {}: {}", protocol, e.getMessage(), e);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message("Could not find users by protocol: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @DeleteMapping("/protocol/{protocol}")
    public ResponseEntity<ApiResponse<String>> removeUsersByProtocol(@PathVariable Long protocol) {
        log.info("Starting removal of impacted users by protocol: {}", protocol);
        try {
            affectedUSerService.removeUsersByProtocol(protocol);

            log.info("Users for protocol {} successfully removed.", protocol);

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(true)
                    .message("Users for protocol " + protocol + " successfully removed.")
                    .build();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error removing users for protocol {}: {}", protocol, e.getMessage(), e);
            ApiResponse<String> response = ApiResponse.<String>builder()
                    .success(false)
                    .message("Error removing users for protocol: " + e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PatchMapping("/protocol/{protocol}")
    public ResponseEntity<ApiResponse<ImpactedUsersOutputDTO>> updateFinishDateByProtocol(@PathVariable Long protocol, @RequestParam LocalDateTime finishDate)
    {
        log.info("Starting update of finish date for protocol: {}", protocol);
        try {
            affectedUSerService.changeEstimationTime(protocol, finishDate);
            log.info("Finish date updated to {} for users of protocol {}", finishDate, protocol);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(true)
                    .message("Finish date updated to " + finishDate + " for users of protocol " + protocol)
                    .data(null)
                    .build();
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.warn("Protocol not found: {}", protocol);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error updating finish date for protocol {}: {}", protocol, e.getMessage(), e);
            ApiResponse<ImpactedUsersOutputDTO> response = ApiResponse.<ImpactedUsersOutputDTO>builder()
                    .success(false)
                    .data(null)
                    .message("Error updating finish date for protocol: " + protocol)
                    .errors(DatabaseErrorParser.parse(e.getMessage()))
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
