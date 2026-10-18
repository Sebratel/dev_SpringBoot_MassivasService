package br.com.sebratel.massivas.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationFilterTest {

    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final CorrelationFilter filter = new CorrelationFilter();

    private String[] run(MockHttpServletRequest request) throws Exception {
        AtomicReference<String[]> seen = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen.set(new String[] {MDC.get("traceId"), MDC.get("requestId")});
            }
        };
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return seen.get();
    }

    @Test
    void putsTheGatewayTraceAndRequestIdInTheLog() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "00-" + TRACE + "-00f067aa0ba902b7-01");
        request.addHeader("X-Request-Id", "req-123");

        String[] seen = run(request);

        assertEquals(TRACE, seen[0]);
        assertEquals("req-123", seen[1]);
    }

    @Test
    void leavesTheFieldsEmptyWithoutTheHeadersAndCleansUpAfterwards() throws Exception {
        String[] seen = run(new MockHttpServletRequest());

        assertNull(seen[0]);
        assertNull(seen[1]);
        assertNull(MDC.get("traceId"));
    }

    @Test
    void ignoresMalformedValuesInsteadOfLoggingThem() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "lixo\nforjado");
        request.addHeader("X-Request-Id", "tem espaco e \n quebra");

        String[] seen = run(request);

        assertNull(seen[0]);
        assertNull(seen[1]);
    }

    @Test
    void cleansTheLogContextAfterTheRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "00-" + TRACE + "-00f067aa0ba902b7-01");

        run(request);

        assertNull(MDC.get("traceId"));
    }
}
