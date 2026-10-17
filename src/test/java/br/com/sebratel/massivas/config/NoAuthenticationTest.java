package br.com.sebratel.massivas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A autenticacao e do API Gateway: o servico nao valida credencial e so e alcancavel pela rede interna.
 * Este teste trava a regressao de alguem recolocar Spring Security aqui.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NoAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsOpen() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void springSecurityIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.security.web.SecurityFilterChain"));
    }
}
