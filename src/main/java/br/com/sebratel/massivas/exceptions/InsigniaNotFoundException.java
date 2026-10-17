package br.com.sebratel.massivas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Cliente existe, mas não possui insígnia cadastrada: não é "recurso inexistente" (404),
// e sim uma inconsistência de dados de negócio.
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsigniaNotFoundException extends RuntimeException {

    public InsigniaNotFoundException(String message) {
        super(message);
    }
}
