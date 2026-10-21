package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivaHistoryCancelamentoInputDTO;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CancelarMassivaHistoryService {

    private final MassivaHistoryJPARepository massivaHistoryJPARepository;

    @Autowired
    public CancelarMassivaHistoryService(MassivaHistoryJPARepository massivaHistoryJPARepository) {
        this.massivaHistoryJPARepository = massivaHistoryJPARepository;
    }

    /**
     * Cancela a massiva no lado Splitters (tabela massiva_history) para o protocolo informado.
     * Idempotente: se já estiver cancelada (ou não existir), retorna 0 sem erro.
     *
     * @return quantidade de linhas efetivamente atualizadas.
     */
    public int cancelar(Long protocol, MassivaHistoryCancelamentoInputDTO input) {
        String cancelledBy = (input != null && input.getCancelledBy() != null) ? input.getCancelledBy() : "reconciliacao-n8n";
        String description = (input != null && input.getCancelDescription() != null)
                ? input.getCancelDescription()
                : "Cancelado via reconciliacao automatica (Elleven ja estava cancelado).";

        int atualizados = massivaHistoryJPARepository.cancelarPorProtocolo(
                protocol, LocalDateTime.now(), cancelledBy, description);

        log.info("Cancelamento de massiva_history para protocolo {}: {} linha(s) atualizada(s).", protocol, atualizados);
        return atualizados;
    }
}
