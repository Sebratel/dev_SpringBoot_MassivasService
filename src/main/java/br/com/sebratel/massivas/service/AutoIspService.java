package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AutoIspSituacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Cruza os contratos de um cliente com os eventos de rede ativos e recentes no AutoISP. */
@Service
@Slf4j
public class AutoIspService {

    private final AutoIspClient client;
    private final int horasRecentes;

    public AutoIspService(AutoIspClient client, @Value("${matrix.historico.horas:6}") int horasRecentes) {
        this.client = client;
        this.horasRecentes = horasRecentes;
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
            var ativos = client.eventosAtivos();
            var eventos = ativos.stream()
                    .filter(e -> AutoIspClient.contratosAfetados(e, meus) > 0)
                    .map(e -> paraDto(e, meus))
                    .collect(Collectors.toList());
            log.info("AutoISP: {} evento(s) ativos no total, {} nos {} contrato(s) consultados", ativos.size(),
                    eventos.size(), meus.size());
            return AutoIspSituacaoDTO.builder().disponivel(true).ativosNoAutoisp(ativos.size()).eventos(eventos)
                    .recentes(recentes(meus, eventos)).recentesHoras(horasRecentes).build();
        } catch (AutoIspClient.IndisponivelException e) {
            log.warn("AutoISP indisponivel: {}", e.getMessage());
            return AutoIspSituacaoDTO.builder().disponivel(false).build();
        }
    }

    /** Encerrados na janela que citam o cliente. Se so esta lista falhar, os ativos continuam valendo. */
    private List<AutoIspSituacaoDTO.Evento> recentes(Set<Long> meus, List<AutoIspSituacaoDTO.Evento> ativosDoCliente) {
        Set<Long> jaListados = ativosDoCliente.stream().map(AutoIspSituacaoDTO.Evento::getId)
                .collect(Collectors.toSet());
        try {
            Instant desde = Instant.now().minus(Duration.ofHours(horasRecentes));
            var lista = client.eventosEncerradosDesde(desde).stream()
                    .filter(e -> !jaListados.contains(e.id()))
                    .filter(e -> AutoIspClient.contratosAfetados(e, meus) > 0)
                    .map(e -> paraDto(e, meus))
                    .collect(Collectors.toList());
            log.info("AutoISP: {} evento(s) encerrado(s) nas ultimas {}h nos contratos consultados", lista.size(),
                    horasRecentes);
            return lista;
        } catch (RuntimeException e) {
            log.warn("AutoISP: encerrados recentes indisponiveis: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private static AutoIspSituacaoDTO.Evento paraDto(AutoIspClient.Evento e, Set<Long> meus) {
        return AutoIspSituacaoDTO.Evento.builder()
                .id(e.id())
                .tipo(e.tipo())
                .status(e.status())
                .inicio(e.inicio())
                .fim(e.fim())
                .contratosAfetados(AutoIspClient.contratosAfetados(e, meus))
                .build();
    }
}
