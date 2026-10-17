package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.model.Employee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallerIdentityTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void callWith(String email, String name) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (email != null) {
            request.addHeader(CallerIdentity.EMAIL_HEADER, email);
        }
        if (name != null) {
            request.addHeader(CallerIdentity.NAME_HEADER, name);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void readsTheUserTheGatewayAuthenticated() {
        callWith("ana@sebratel.com.br", "Ana+Jo%C3%A3o");

        Employee user = CallerIdentity.retrieveUserData();

        assertEquals("ana@sebratel.com.br", user.email());
        assertEquals("Ana João", user.name());
    }

    @Test
    void withoutTheGatewayHeaderTheUserIsEmpty() {
        callWith(null, null);

        Employee user = CallerIdentity.retrieveUserData();

        assertEquals("", user.email());
        assertEquals("", user.name());
    }

    @Test
    void outsideARequestTheUserIsEmpty() {
        assertEquals("", CallerIdentity.retrieveUserData().email());
    }
}
