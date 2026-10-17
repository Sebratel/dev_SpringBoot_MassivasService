package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.ImpactedUsersInputDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class EnviarListaDeAfetadosParaNativeService {
    public ImpactedUsersInputDTO executar(ImpactedUsersInputDTO input) {
       try{

       } catch (RuntimeException e) {
           throw new RuntimeException(e);
       }
        return input;
    }

    private Mono<? extends Throwable> handleHttpError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    log.error("Erro HTTP na etapa {}: Status {} - Body: {}", "conclusão", response.statusCode(), body);
                    return Mono.error(new RuntimeException("Falha na integração Elleven: " + body));
                });
    }
}
