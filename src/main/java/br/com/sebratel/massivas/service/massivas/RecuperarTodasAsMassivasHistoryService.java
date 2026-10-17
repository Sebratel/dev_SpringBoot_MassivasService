package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivaHistoryOutputDTO;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecuperarTodasAsMassivasHistoryService {

    private final MassivaHistoryJPARepository massivaHistoryJPARepository;

    @Autowired
    public RecuperarTodasAsMassivasHistoryService(MassivaHistoryJPARepository massivaHistoryJPARepository) {
        this.massivaHistoryJPARepository = massivaHistoryJPARepository;
    }

    public List<MassivaHistoryOutputDTO> executar() {
        return this.massivaHistoryJPARepository.findAll()
                .stream().map(entity -> new MassivaHistoryOutputDTO(
                        entity.getId(),
                        entity.getProtocol() != null ? String.valueOf(entity.getProtocol()) : null,
                        entity.getAssignmentId() != null ? String.valueOf(entity.getAssignmentId()) : null,
                        entity.getStatus(),
                        entity.getTitle(),
                        entity.getAccessPointCode(),
                        entity.getAffectedClients(),
                        entity.getOpenedAt(),
                        entity.getClosedAt(),
                        entity.getUpdatedAt(),
                        entity.getClosedBy(),
                        entity.getCloseDescription(),
                        entity.getSource()
                )).toList();
    }
}
