package br.com.sebratel.massivas.service.massivas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Idempotência em memória para a abertura de protocolos no Elleven.
 *
 * <p>Motivo: cada POST cria um protocolo novo no Elleven (sem deduplicação). Se o cliente
 * repete a chamada (retry após timeout de 10s, em que o Elleven já criou; ou duplo clique),
 * geram-se protocolos duplicados. Com uma {@code Idempotency-Key} por tentativa, a primeira
 * chamada é encaminhada e a <b>resposta de sucesso</b> é cacheada; repetições com a mesma
 * chave, dentro da janela de TTL, recebem a resposta cacheada <b>sem reencaminhar</b>.</p>
 *
 * <p>Só cacheia respostas 2xx: falhas reais (ex.: BAD_GATEWAY) e exceções não são guardadas,
 * permitindo um retry legítimo. Sem chave (header ausente) o comportamento é o de antes.</p>
 */
@Component
@Slf4j
public class MassivaIdempotencyStore {

    /** Janela de deduplicação. Cobre o timeout de 10s do cliente com folga. */
    private static final long TTL_MS = 120_000L;

    private final Map<String, Holder> store = new ConcurrentHashMap<>();

    private static final class Holder {
        final Object lock = new Object();
        volatile ResponseEntity<?> cached;
        volatile long createdAt = System.currentTimeMillis();
    }

    /**
     * Executa a ação com deduplicação pela chave. Requisições concorrentes com a mesma chave
     * são serializadas (a segunda espera e recebe o resultado da primeira).
     *
     * @param key    valor do header {@code Idempotency-Key} (null/blank = sem idempotência)
     * @param action a chamada real ao Elleven, que devolve o {@link ResponseEntity}
     */
    public <T> ResponseEntity<T> execute(String key, Supplier<ResponseEntity<T>> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        final String k = key.trim();
        purgeExpired();

        final Holder holder = store.computeIfAbsent(k, x -> new Holder());
        synchronized (holder.lock) {
            if (holder.cached != null) {
                log.info("Idempotency HIT — devolvendo resposta cacheada sem reencaminhar. [key={}]", k);
                @SuppressWarnings("unchecked")
                final ResponseEntity<T> cached = (ResponseEntity<T>) holder.cached;
                return cached;
            }

            final ResponseEntity<T> result;
            try {
                result = action.get();
            } catch (RuntimeException ex) {
                store.remove(k); // não guarda tentativa que explodiu — permite retry
                throw ex;
            }

            if (result != null && result.getStatusCode().is2xxSuccessful()) {
                holder.cached = result;
                holder.createdAt = System.currentTimeMillis();
            } else {
                store.remove(k); // não guarda falha (ex.: BAD_GATEWAY) — permite retry
            }
            return result;
        }
    }

    private void purgeExpired() {
        final long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }
}
