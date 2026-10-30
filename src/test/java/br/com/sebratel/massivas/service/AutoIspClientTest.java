package br.com.sebratel.massivas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AutoIspClientTest {

    private static final String LOGIN = "https://autoisp.exemplo/api/auth/v2/request_token";
    private static final String EVENTOS = "https://autoisp.exemplo/api/massives/v1/events";
    private static final String TOKEN = "tok-abcdefghijklmnopqrstuvwxyz";

    private MockRestServiceServer servidor;

    private AutoIspClient cliente(String usuario, String senha, long cacheSegundos) {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        return new AutoIspClient(LOGIN, EVENTOS, usuario, senha, cacheSegundos, builder.build());
    }

    private void esperaLogin(ExpectedCount vezes) {
        servidor.expect(vezes, requestTo(LOGIN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"username\":\"api@exemplo\",\"password\":\"segredo\"}"))
                .andRespond(withSuccess("{\"response\":{\"token\":\"" + TOKEN + "\",\"expires_in\":3600}}",
                        MediaType.APPLICATION_JSON));
    }

    private void esperaEventos(String status, String corpo) {
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString(EVENTOS + "?filters=")))
                .andExpect(requestTo(org.hamcrest.Matchers.containsString(status)))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }

    private static String evento(long id, String tipo, String status, long contrato) {
        return "{\"id\":" + id + ",\"event_type\":\"" + tipo + "\",\"admin_status\":\"" + status
                + "\",\"start_at\":\"2026-10-30T10:00:00\",\"resources\":[{\"contract_id\":" + contrato
                + ",\"pppoe_username\":\"fulano\"}]}";
    }

    @Test
    @DisplayName("Sem usuario ou senha o cliente se declara nao configurado")
    void naoConfigurado() {
        assertFalse(cliente("", "", 30).configurado());
        assertTrue(cliente("api@exemplo", "segredo", 30).configurado());
    }

    @Test
    @DisplayName("Loga uma vez, lista os 4 status ativos e junta os eventos sem repetir")
    void listaEventosDeTodosOsStatus() {
        AutoIspClient c = cliente("api@exemplo", "segredo", 30);
        esperaLogin(ExpectedCount.once());
        esperaEventos("new", "{\"data\":[" + evento(1, "olt_down", "new", 10) + "]}");
        esperaEventos("acknowledged", "{\"data\":[" + evento(2, "port_down", "acknowledged", 20) + "]}");
        esperaEventos("open", "{\"data\":[" + evento(1, "olt_down", "open", 10) + "]}");
        esperaEventos("in_progress", "{\"data\":[]}");

        List<AutoIspClient.Evento> eventos = c.eventosAtivos();

        assertEquals(2, eventos.size());
        assertEquals("olt_down", eventos.get(0).tipo());
        assertEquals(Long.valueOf(10), eventos.get(0).recursos().get(0).contratoId());
        servidor.verify();
    }

    @Test
    @DisplayName("Sem eventos nos status em minusculas, tenta em maiusculas")
    void fallbackParaMaiusculas() {
        AutoIspClient c = cliente("api@exemplo", "segredo", 30);
        esperaLogin(ExpectedCount.once());
        for (String s : List.of("new", "acknowledged", "open", "in_progress")) {
            esperaEventos(s, "{\"data\":[]}");
        }
        servidor.expect(requestTo(org.hamcrest.Matchers.containsString("NEW")))
                .andRespond(withSuccess("{\"data\":[" + evento(5, "olt_down", "NEW", 10) + "]}", MediaType.APPLICATION_JSON));
        for (String s : List.of("OPEN", "IN_PROGRESS", "ACKNOWLEDGED", "PENDING", "ACTIVE")) {
            servidor.expect(requestTo(org.hamcrest.Matchers.containsString(s)))
                    .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        }

        List<AutoIspClient.Evento> eventos = c.eventosAtivos();

        assertEquals(1, eventos.size());
        assertEquals(5L, eventos.get(0).id());
    }

    @Test
    @DisplayName("Sem eventos em nenhum status, lista sem filtro")
    void fallbackSemFiltro() {
        AutoIspClient c = cliente("api@exemplo", "segredo", 30);
        esperaLogin(ExpectedCount.once());
        for (String s : List.of("new", "acknowledged", "open", "in_progress", "NEW", "OPEN", "IN_PROGRESS",
                "ACKNOWLEDGED", "PENDING", "ACTIVE")) {
            servidor.expect(requestTo(org.hamcrest.Matchers.containsString("filters=") ))
                    .andExpect(requestTo(org.hamcrest.Matchers.containsString(s)))
                    .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
        }
        servidor.expect(requestTo(EVENTOS + "?page=1&per_page=1000"))
                .andRespond(withSuccess("{\"data\":[" + evento(6, "port_down", "open", 20) + "]}", MediaType.APPLICATION_JSON));

        List<AutoIspClient.Evento> eventos = c.eventosAtivos();

        assertEquals(1, eventos.size());
        assertEquals(6L, eventos.get(0).id());
    }

    @Test
    @DisplayName("A segunda chamada dentro do cache nao vai ao AutoISP")
    void usaCache() {
        AutoIspClient c = cliente("api@exemplo", "segredo", 60);
        esperaLogin(ExpectedCount.once());
        for (String s : List.of("new", "acknowledged", "open", "in_progress")) {
            esperaEventos(s, "[" + evento(1, "olt_down", s, 10) + "]");
        }

        c.eventosAtivos();
        c.eventosAtivos();

        servidor.verify();
    }

    @Test
    @DisplayName("Login recusado vira IndisponivelException, sem vazar o motivo")
    void loginRecusado() {
        AutoIspClient c = cliente("api@exemplo", "segredo", 30);
        servidor.expect(ExpectedCount.manyTimes(), requestTo(LOGIN))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AutoIspClient.IndisponivelException.class, c::eventosAtivos);
    }

    @Test
    @DisplayName("Conta so os contratos do cliente citados pelo evento")
    void contratosAfetados() {
        var evento = new AutoIspClient.Evento(1, "olt_down", "new", null, List.of(
                new AutoIspClient.Recurso(10L, "a"), new AutoIspClient.Recurso(10L, "b"),
                new AutoIspClient.Recurso(30L, "c"), new AutoIspClient.Recurso(null, "d")));

        assertEquals(1, AutoIspClient.contratosAfetados(evento, Set.of(10L, 20L)));
        assertEquals(0, AutoIspClient.contratosAfetados(evento, Set.of(99L)));
    }

    @Test
    @DisplayName("O token e achado em varios formatos de resposta")
    void extraiToken() throws Exception {
        ObjectMapper m = new ObjectMapper();
        assertEquals(TOKEN, AutoIspClient.extrairToken(m.readTree("{\"access_token\":\"" + TOKEN + "\"}")));
        assertEquals(TOKEN, AutoIspClient.extrairToken(m.readTree("{\"response\":{\"jwt\":\"" + TOKEN + "\"}}")));
        assertEquals(TOKEN, AutoIspClient.extrairToken(m.readTree("\"" + TOKEN + "\"")));
        assertNull(AutoIspClient.extrairToken(m.readTree("{\"mensagem\":\"ok\"}")));
    }
}
