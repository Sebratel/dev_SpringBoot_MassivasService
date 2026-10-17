package br.com.sebratel.massivas.utils;

import br.com.sebratel.massivas.model.Employee;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Quem chamou, como entregue pelo API Gateway. A autenticacao e feita la; o servico so le o resultado:
 * {@code X-User-Email} e {@code X-User-Name} (este codificado como URL, por causa de acentos).
 */
public final class CallerIdentity {

    public static final String EMAIL_HEADER = "X-User-Email";
    public static final String NAME_HEADER = "X-User-Name";

    private static final Logger LOGGER = LoggerFactory.getLogger(CallerIdentity.class);

    private CallerIdentity() {
    }

    /** Dados do usuario da requisicao atual; vazio (e-mail e nome em branco) se o gateway nao informou. */
    public static Employee retrieveUserData() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String email = request.getHeader(EMAIL_HEADER);
            if (email != null && !email.isBlank()) {
                String name = decode(request.getHeader(NAME_HEADER));
                LOGGER.info("Processando requisição para: {} ({})", name, email);
                return new Employee(email, name);
            }
        }
        LOGGER.warn("Requisição sem identidade do gateway ({} ausente)", EMAIL_HEADER);
        return new Employee("", "");
    }

    private static String decode(String value) {
        return value == null ? "" : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
