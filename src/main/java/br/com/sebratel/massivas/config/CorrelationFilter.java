package br.com.sebratel.massivas.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rastreamento: le o {@code traceparent} (W3C) e o {@code X-Request-Id} que o API Gateway envia e os poe no log (MDC),
 * para uma requisicao ser seguida do cliente ate o log deste servico. O servico nao cria rastro: sem o cabecalho
 * (chamada direta, health check) os campos ficam vazios.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";

    private static final Pattern TRACEPARENT = Pattern.compile("^00-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}$");
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        putIfValid(TRACE_ID, traceIdOf(request.getHeader("traceparent")));
        String requestId = request.getHeader("X-Request-Id");
        putIfValid(REQUEST_ID, requestId != null && SAFE_ID.matcher(requestId).matches() ? requestId : null);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }

    private static String traceIdOf(String traceparent) {
        if (traceparent == null) {
            return null;
        }
        Matcher matcher = TRACEPARENT.matcher(traceparent.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static void putIfValid(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }
}
