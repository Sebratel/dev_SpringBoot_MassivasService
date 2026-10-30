package br.com.sebratel.massivas.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eventos de rede ativos no AutoISP (queda de OLT, porta, etc.), do mesmo jeito que o painel do Splitters os
 * lista: login por POST com usuario e senha, depois GET de eventos filtrado por status administrativo, com o
 * token como Bearer. As credenciais vem das variaveis AUTOISP_* e nunca saem deste servidor.
 */
@Component
@Slf4j
public class AutoIspClient {

    /** Status que contam como alteracao de rede em andamento. */
    static final List<String> STATUS_ATIVOS = List.of("new", "acknowledged", "open", "in_progress");
    /** Se nenhum evento vier com os status acima, a API pode estar usando outra caixa/nome (como o Splitters tenta). */
    static final List<String> STATUS_ALTERNATIVOS = List.of("NEW", "OPEN", "IN_PROGRESS", "ACKNOWLEDGED", "PENDING",
            "ACTIVE");

    private static final List<String> CHAVES_LISTA = List.of("data", "items", "list", "results", "events", "records",
            "content", "massives", "massive_events", "rows");
    private static final List<String> CHAVES_TOKEN = List.of("token", "access_token", "accessToken", "jwt",
            "id_token", "idToken", "bearer");
    private static final String STATUS_ENCERRADO = "closed";
    private static final int MAX_PAGINAS_ENCERRADOS = 5;
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int POR_PAGINA = 1000;
    private static final long TOKEN_PADRAO_SEGUNDOS = 3600;

    /** Contrato (ERP) e usuario PPPoE de um recurso afetado por um evento. */
    public record Recurso(Long contratoId, String pppoe) {
    }

    public record Evento(long id, String tipo, String status, String inicio, String fim, List<Recurso> recursos) {
    }

    /** O AutoISP nao respondeu ou recusou o login: quem chama mostra "indisponivel" em vez de falhar. */
    public static class IndisponivelException extends RuntimeException {
        public IndisponivelException(String mensagem) {
            super(mensagem);
        }
    }

    private final RestClient http;
    private final String urlLogin;
    private final String urlEventos;
    private final String usuario;
    private final String senha;
    private final Duration validadeCache;

    private String token;
    private Instant tokenAte = Instant.EPOCH;
    private List<Evento> cache = List.of();
    private Instant cacheAte = Instant.EPOCH;
    private List<Evento> cacheEncerrados = List.of();
    private Instant cacheEncerradosAte = Instant.EPOCH;

    @Autowired
    public AutoIspClient(@Value("${autoisp.auth-url:}") String urlLogin,
                         @Value("${autoisp.events-url:}") String urlEventos,
                         @Value("${autoisp.user:}") String usuario,
                         @Value("${autoisp.password:}") String senha,
                         @Value("${autoisp.cache-segundos:30}") long cacheSegundos,
                         RestClient.Builder builder) {
        this(urlLogin, urlEventos, usuario, senha, cacheSegundos, builder.requestFactory(comTimeouts()).build());
    }

    /** Usado nos testes, com um RestClient ligado a um servidor simulado. */
    AutoIspClient(String urlLogin, String urlEventos, String usuario, String senha, long cacheSegundos,
                  RestClient http) {
        this.urlLogin = urlLogin.trim();
        this.urlEventos = urlEventos.trim();
        this.usuario = usuario;
        this.senha = senha;
        this.validadeCache = Duration.ofSeconds(cacheSegundos);
        this.http = http;
    }

    private static SimpleClientHttpRequestFactory comTimeouts() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(8000);
        return fabrica;
    }

    public boolean configurado() {
        return !urlLogin.isEmpty() && !urlEventos.isEmpty() && !usuario.isBlank() && !senha.isBlank();
    }

    /**
     * Eventos ativos. O resultado fica em cache por alguns segundos: a mesma lista serve a todas as consultas
     * de atendentes no periodo, em vez de uma ida ao AutoISP por ligacao.
     */
    public synchronized List<Evento> eventosAtivos() {
        if (Instant.now().isBefore(cacheAte)) {
            return cache;
        }
        Map<Long, Evento> unicos = new LinkedHashMap<>();
        int falhas = juntar(STATUS_ATIVOS, unicos);
        if (falhas == STATUS_ATIVOS.size()) {
            throw new IndisponivelException("AutoISP indisponivel");
        }
        if (unicos.isEmpty()) {
            juntar(STATUS_ALTERNATIVOS, unicos);
        }
        if (unicos.isEmpty()) {
            try {
                for (Evento e : eventos(null)) {
                    unicos.putIfAbsent(e.id(), e);
                }
            } catch (RuntimeException e) {
                log.warn("AutoISP: falha na listagem sem filtro: {}", e.getMessage());
            }
        }
        cache = List.copyOf(unicos.values());
        cacheAte = Instant.now().plus(validadeCache);
        return cache;
    }

    /** Junta em {@code destino} os eventos de cada status; devolve quantos status falharam. */
    private int juntar(List<String> statuses, Map<Long, Evento> destino) {
        int falhas = 0;
        for (String status : statuses) {
            try {
                for (Evento e : eventos(status)) {
                    destino.putIfAbsent(e.id(), e);
                }
            } catch (RuntimeException e) {
                falhas++;
                log.warn("AutoISP: falha ao listar eventos com status {}: {}", status, e.getMessage());
            }
        }
        return falhas;
    }

    /**
     * Eventos encerrados cujo fim (ou, sem fim, o inicio) e de {@code desde} em diante. A API so e conhecida por
     * filtrar por status, entao a lista de encerrados e baixada (ate {@value #MAX_PAGINAS_ENCERRADOS} paginas) e a
     * janela e aplicada aqui; o resultado fica em cache por mais tempo que o dos ativos.
     */
    public synchronized List<Evento> eventosEncerradosDesde(Instant desde) {
        if (!Instant.now().isBefore(cacheEncerradosAte)) {
            List<Evento> todos = new ArrayList<>();
            for (int pagina = 1; pagina <= MAX_PAGINAS_ENCERRADOS; pagina++) {
                List<Evento> lote = eventos(STATUS_ENCERRADO, pagina);
                todos.addAll(lote);
                if (lote.size() < POR_PAGINA) {
                    break;
                }
            }
            cacheEncerrados = List.copyOf(todos);
            cacheEncerradosAte = Instant.now().plus(validadeCache.multipliedBy(2));
            log.info("AutoISP: {} evento(s) encerrado(s) lidos", cacheEncerrados.size());
        }
        return cacheEncerrados.stream().filter(e -> recente(e, desde)).toList();
    }

    private static boolean recente(Evento e, Instant desde) {
        Instant fim = instante(e.fim());
        Instant referencia = fim != null ? fim : instante(e.inicio());
        return referencia != null && !referencia.isBefore(desde);
    }

    /** Data/hora da API: com fuso (ISO-8601) ou sem fuso, que se entende como horario de Sao Paulo. */
    static Instant instante(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim().replace(' ', 'T');
        try {
            return OffsetDateTime.parse(t).toInstant();
        } catch (DateTimeParseException ignorado) {
            // sem fuso: tenta como horario local
        }
        try {
            return LocalDateTime.parse(t).atZone(ZONA).toInstant();
        } catch (DateTimeParseException ignorado) {
            return null;
        }
    }

    /** Eventos com o status administrativo dado, ou todos (sem filtro) quando {@code status} e null. */
    private List<Evento> eventos(String status) {
        return eventos(status, 1);
    }

    private List<Evento> eventos(String status, int pagina) {
        String filtro = "[{\"field\":\"admin_status\",\"op\":\"==\",\"value\":\"" + status + "\"}]";
        RestClient.RequestHeadersSpec<?> pedido = status == null
                ? http.get().uri(urlEventos + "?page=" + pagina + "&per_page=" + POR_PAGINA)
                : http.get().uri(urlEventos + "?filters={f}&page=" + pagina + "&per_page=" + POR_PAGINA, filtro);
        JsonNode corpo = pedido
                .header("Authorization", "Bearer " + tokenValido())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
        List<Evento> eventos = new ArrayList<>();
        for (JsonNode bruto : listaDe(corpo)) {
            Evento e = paraEvento(bruto);
            if (e != null) {
                eventos.add(e);
            }
        }
        return eventos;
    }

    private String tokenValido() {
        if (token != null && Instant.now().isBefore(tokenAte)) {
            return token;
        }
        JsonNode resposta;
        try {
            resposta = http.post()
                    .uri(urlLogin)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", usuario, "password", senha))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException e) {
            // so o tipo e o status: o corpo da resposta e o da requisicao nunca vao para o log (tem a senha)
            log.warn("AutoISP: login falhou ({})", e.getClass().getSimpleName());
            throw new IndisponivelException("Login no AutoISP falhou");
        }
        String novo = extrairToken(resposta);
        if (novo == null) {
            throw new IndisponivelException("AutoISP nao devolveu token");
        }
        long segundos = segundosDeValidade(resposta);
        long folga = Math.min(300, Math.max(60, segundos * 15 / 100));
        token = novo;
        tokenAte = Instant.now().plusSeconds(Math.max(30, segundos - folga));
        return token;
    }

    static String extrairToken(JsonNode raiz) {
        if (raiz == null) {
            return null;
        }
        if (raiz.isTextual()) {
            String t = raiz.asText().trim();
            return t.length() > 20 ? t : null;
        }
        ArrayDeque<JsonNode> fila = new ArrayDeque<>();
        fila.add(raiz);
        while (!fila.isEmpty()) {
            JsonNode atual = fila.poll();
            if (atual.isArray()) {
                atual.forEach(fila::add);
                continue;
            }
            if (!atual.isObject()) {
                continue;
            }
            for (String chave : CHAVES_TOKEN) {
                JsonNode v = atual.get(chave);
                if (v != null && v.isTextual() && v.asText().trim().length() > 12) {
                    return v.asText().trim();
                }
            }
            atual.forEach(v -> {
                if (v.isContainerNode()) {
                    fila.add(v);
                }
            });
        }
        return null;
    }

    private static long segundosDeValidade(JsonNode raiz) {
        if (raiz != null && raiz.isObject()) {
            for (JsonNode candidato : new JsonNode[]{raiz.get("expires_in"), raiz.path("response").get("expires_in"),
                    raiz.get("expiresIn")}) {
                if (candidato != null && candidato.canConvertToLong() && candidato.asLong() > 0) {
                    return candidato.asLong();
                }
            }
        }
        return TOKEN_PADRAO_SEGUNDOS;
    }

    /** A API devolve a lista ora direto, ora dentro de "data", "items" etc. (mesma tolerancia do Splitters). */
    static List<JsonNode> listaDe(JsonNode raiz) {
        List<JsonNode> saida = new ArrayList<>();
        if (raiz == null) {
            return saida;
        }
        if (raiz.isArray()) {
            raiz.forEach(saida::add);
            return saida;
        }
        if (!raiz.isObject()) {
            return saida;
        }
        for (String chave : CHAVES_LISTA) {
            JsonNode v = raiz.get(chave);
            if (v == null) {
                continue;
            }
            List<JsonNode> interna = listaDe(v);
            if (!interna.isEmpty()) {
                return interna;
            }
        }
        return saida;
    }

    private static Evento paraEvento(JsonNode bruto) {
        if (bruto == null || !bruto.isObject()) {
            return null;
        }
        long id = longDe(bruto, "id", "ID", "event_id", "eventId", "massive_id", "massiveId");
        if (id <= 0) {
            return null;
        }
        List<Recurso> recursos = new ArrayList<>();
        JsonNode lista = bruto.get("resources");
        if (lista != null && lista.isArray()) {
            for (JsonNode r : lista) {
                long contrato = longDe(r, "contractId", "contract_id");
                String pppoe = textoDe(r, "pppoeUsername", "pppoe_username", "username");
                recursos.add(new Recurso(contrato > 0 ? contrato : null, pppoe));
            }
        }
        return new Evento(id,
                textoDe(bruto, "eventType", "event_type", "type"),
                textoDe(bruto, "adminStatus", "admin_status"),
                textoDe(bruto, "startAt", "start_at"),
                textoDe(bruto, "endAt", "end_at"),
                recursos);
    }

    private static long longDe(JsonNode no, String... chaves) {
        for (String c : chaves) {
            JsonNode v = no.get(c);
            if (v == null || v.isNull()) {
                continue;
            }
            if (v.canConvertToLong() && v.isNumber()) {
                return v.asLong();
            }
            if (v.isTextual()) {
                try {
                    return Long.parseLong(v.asText().trim());
                } catch (NumberFormatException ignorado) {
                    // texto que nao e numero: tenta a proxima chave
                }
            }
        }
        return 0;
    }

    private static String textoDe(JsonNode no, String... chaves) {
        for (String c : chaves) {
            JsonNode v = no.get(c);
            if (v != null && !v.isNull() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    /** Contratos de {@code contratos} citados por algum recurso do evento. */
    static long contratosAfetados(Evento evento, Set<Long> contratos) {
        return evento.recursos().stream()
                .map(Recurso::contratoId)
                .filter(c -> c != null && contratos.contains(c))
                .distinct()
                .count();
    }
}
