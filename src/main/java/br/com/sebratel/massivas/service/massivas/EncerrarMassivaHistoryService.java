package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivaHistoryEncerramentoInputDTO;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EncerrarMassivaHistoryService {

    private final MassivaHistoryJPARepository massivaHistoryJPARepository;

    @Autowired
    public EncerrarMassivaHistoryService(MassivaHistoryJPARepository massivaHistoryJPARepository) {
        this.massivaHistoryJPARepository = massivaHistoryJPARepository;
    }

    /**
     * Encerra a massiva no lado Splitters (tabela massiva_history) para o protocolo informado.
     * Idempotente: se já estiver encerrada (ou não existir), retorna 0 sem erro.
     *
     * @return quantidade de linhas efetivamente atualizadas.
     */
    public int encerrar(Long protocol, MassivaHistoryEncerramentoInputDTO input) {
        String closedBy = (input != null && input.getClosedBy() != null) ? input.getClosedBy() : "reconciliacao-n8n";
        String description = (input != null && input.getCloseDescription() != null)
                ? input.getCloseDescription()
                : "Encerrado via reconciliacao automatica (Voalle ja estava fechado).";

        int atualizados = massivaHistoryJPARepository.encerrarPorProtocolo(
                protocol, LocalDateTime.now(), closedBy, description);

        log.info("Encerramento de massiva_history para protocolo {}: {} linha(s) atualizada(s).", protocol, atualizados);
        return atualizados;
    }
}
