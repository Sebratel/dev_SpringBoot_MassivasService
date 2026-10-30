package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AutoIspSituacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Cruza os contratos de um cliente com os eventos de rede ativos no AutoISP. */
@Service
@Slf4j
public class AutoIspService {

    private final AutoIspClient client;

    public AutoIspService(AutoIspClient client) {
        this.client = client;
    }

    /**
     * Situacao no AutoISP dos contratos informados, ou {@code null} se o AutoISP nao esta configurado neste
     * servidor (o bloco some da resposta). Falha do AutoISP nunca derruba a consulta de massiva.
     */
    public AutoIspSituacaoDTO situacao(Collection<Long> contratos) {
        if (!client.configurado()) {
            return null;
        }
        Set<Long> meus = Set.copyOf(contratos);
        try {
            var eventos = client.eventosAtivos().stream()
                    .filter(e -> AutoIspClient.contratosAfetados(e, meus) > 0)
                    .map(e -> AutoIspSituacaoDTO.Evento.builder()
                            .id(e.id())
                            .tipo(e.tipo())
                            .status(e.status())
                            .inicio(e.inicio())
                            .contratosAfetados(AutoIspClient.contratosAfetados(e, meus))
                            .build())
                    .collect(Collectors.toList());
            log.info("AutoISP: {} evento(s) ativos nos {} contrato(s) consultados", eventos.size(), meus.size());
            return AutoIspSituacaoDTO.builder().disponivel(true).eventos(eventos).build();
        } catch (AutoIspClient.IndisponivelException e) {
            log.warn("AutoISP indisponivel: {}", e.getMessage());
            return AutoIspSituacaoDTO.builder().disponivel(false).build();
        }
    }
}
