package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.EllevenApiResponseDTO;
import br.com.sebratel.massivas.utils.GetToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class GetAllMassivesService {

    private final WebClient webClient;

    public GetAllMassivesService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://erp-staging.sebratel.net.br:45701").build();
    }

    public EllevenApiResponseDTO getAllSolicitations() {
        // A URL completa e exata da sua cURL
        String fullUrl = "/api/v1/ServiceDesk/AssignmentIncidents/GetAllSolicitations?OrderBy=%5B%7B%22PropertyName%22%3A%22id%22%2C%22Dir%22%3A%22d%22%7D%5D&Page=1&PageSize=20&Filter=%7B%22Connector%22%3A%22And%22%2C%22Values%22%3A%5B%7B%22PropertyName%22%3A%22assignment.beginningDate%22%2C%22Value%22%3A%222026-03-09%2000%3A00%3A00%22%2C%22Operation%22%3A%22greaterThanOrEquals%22%7D%2C%7B%22PropertyName%22%3A%22assignment.beginningDate%22%2C%22Value%22%3A%222026-03-09%2023%3A59%3A59%22%2C%22Operation%22%3A%22LessThanOrEquals%22%7D%5D%7D&slaType=null";

        try {
            log.info("Executando chamada GET idêntica à cURL fornecida.");

            return webClient.get()
                    .uri(fullUrl) // Passando a URL como string bruta, o Spring não tentará interpretar
                    .header(HttpHeaders.AUTHORIZATION, GetToken.retrieve())
                    .header(HttpHeaders.ACCEPT, "*/*")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Origin", "https://erp-staging.sebratel.net.br")
                    .header("Referer", "https://erp-staging.sebratel.net.br/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36")
                    .header("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .retrieve()
                    .bodyToMono(EllevenApiResponseDTO.class)
                    .block();

        } catch (Exception e) {
            log.error("Erro fatal na requisição cURL: {}", e.getMessage());
            throw new RuntimeException("Falha na execução da chamada", e);
        }
    }
}