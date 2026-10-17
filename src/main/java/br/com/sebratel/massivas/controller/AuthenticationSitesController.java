package br.com.sebratel.massivas.controller;

import br.com.sebratel.massivas.dto.ApiResponse;
import br.com.sebratel.massivas.dto.AuthenticationSitesOutputDTO;
import br.com.sebratel.massivas.service.AuthenticationSitesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("api/v1/sites")
@Slf4j
@RestController
public class AuthenticationSitesController {

    private final AuthenticationSitesService authenticationSitesService;

    @Autowired
    public AuthenticationSitesController(AuthenticationSitesService authenticationSitesService) {
        this.authenticationSitesService = authenticationSitesService;
    }

    @GetMapping
    public ApiResponse<List<AuthenticationSitesOutputDTO>> getSites(@RequestParam String title) {
        return ApiResponse.<List<AuthenticationSitesOutputDTO>>builder()
                .success(true)
                .data(authenticationSitesService.execute(title))
                .message("Sites retrivied sucessfully")
                .build();
    }

    /** Busca por título (contém) para o seletor de site — usado na abertura de protocolo de backbone. */
    @GetMapping("/search")
    public ApiResponse<List<AuthenticationSitesOutputDTO>> searchSites(@RequestParam String q) {
        return ApiResponse.<List<AuthenticationSitesOutputDTO>>builder()
                .success(true)
                .data(authenticationSitesService.search(q))
                .message("Sites retrieved successfully")
                .build();
    }
}
