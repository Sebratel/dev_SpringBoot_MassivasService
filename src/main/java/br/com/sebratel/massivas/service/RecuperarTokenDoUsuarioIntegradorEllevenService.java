package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.EllevenCredentialsDTO;
import br.com.sebratel.massivas.dto.splitters.RecuperarTokenEllevenOutputDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RecuperarTokenDoUsuarioIntegradorEllevenService {

    // Margem de segurança para renovar o token antes de expirar de fato (em segundos).
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 60;
    // TTL de fallback caso a API não retorne expires_in.
    private static final long FALLBACK_TTL_SECONDS = 3500;

    private final WebClient webClient;
    private final EllevenCredentialsDTO credentials;

    // Cache em memória do token (sem Redis). Um único token de integração é compartilhado
    // por todas as chamadas; guardamos o valor e o instante de expiração.
    private volatile RecuperarTokenEllevenOutputDTO cachedToken;
    private volatile long expiresAtEpochMillis;

    @Autowired
    public RecuperarTokenDoUsuarioIntegradorEllevenService(WebClient webClient, EllevenCredentialsDTO credentials) {
        this.webClient = webClient;
        this.credentials = credentials;
    }


    public RecuperarTokenEllevenOutputDTO executar() {
        RecuperarTokenEllevenOutputDTO current = cachedToken;
        if (current != null && System.currentTimeMillis() < expiresAtEpochMillis) {
            return current;
        }
        return refreshToken();
    }

    private synchronized RecuperarTokenEllevenOutputDTO refreshToken() {
        // Double-check: outra thread pode ter renovado enquanto aguardávamos o lock.
        if (cachedToken != null && System.currentTimeMillis() < expiresAtEpochMillis) {
            return cachedToken;
        }

        log.info("Buscando novo token de integração Elleven (cache expirado ou vazio).");
        RecuperarTokenEllevenOutputDTO token = fetchToken();

        long ttlSeconds = (token.expiresIn() != null && token.expiresIn() > EXPIRY_SAFETY_MARGIN_SECONDS)
                ? token.expiresIn() - EXPIRY_SAFETY_MARGIN_SECONDS
                : FALLBACK_TTL_SECONDS;

        this.cachedToken = token;
        this.expiresAtEpochMillis = System.currentTimeMillis() + (ttlSeconds * 1000L);
        return token;
    }

    private RecuperarTokenEllevenOutputDTO fetchToken() {
        String ellevenTokenUrl = "https://erp.sebratel.net.br:45700/connect/token";
        return webClient.post()
                .uri(ellevenTokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(credentials.toFormData()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleHttpError("conclusão", response))
                .bodyToMono(RecuperarTokenEllevenOutputDTO.class)
                .block();
    }

    public synchronized void invalidateToken() {
        this.cachedToken = null;
        this.expiresAtEpochMillis = 0;
        log.warn("Cache do token de integração invalidado (ex.: erro 401 na API).");
    }

    private Mono<? extends Throwable> handleHttpError(String context, ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.error("Erro HTTP na etapa {}: Status {} - Body: {}", context, response.statusCode(), body);
                    return Mono.error(new RuntimeException("Falha na integração Elleven: " + body));
                });
    }
}
