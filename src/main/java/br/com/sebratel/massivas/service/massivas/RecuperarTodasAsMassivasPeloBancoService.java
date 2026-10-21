package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivasBFFOutputDTO;
import br.com.sebratel.massivas.repository.erp.massivas.RecuperarTodasAsMassivasPeloBancoRepository;
import br.com.sebratel.massivas.repository.erp.projections.RecuperarTodasAsMassivasProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecuperarTodasAsMassivasPeloBancoService {

    private final RecuperarTodasAsMassivasPeloBancoRepository recuperarTodasAsMassivasPeloBancoRepository;

    @Autowired
    public RecuperarTodasAsMassivasPeloBancoService(RecuperarTodasAsMassivasPeloBancoRepository recuperarTodasAsMassivasPeloBancoRepository) {
        this.recuperarTodasAsMassivasPeloBancoRepository = recuperarTodasAsMassivasPeloBancoRepository;
    }

    public List<MassivasBFFOutputDTO> executar() {
        return toDTOs(this.recuperarTodasAsMassivasPeloBancoRepository.findActiveAssignments());
    }

    public List<MassivasBFFOutputDTO> executarPorProtocolos(List<Long> protocolos) {
        if (protocolos == null || protocolos.isEmpty()) {
            return List.of();
        }
        return toDTOs(this.recuperarTodasAsMassivasPeloBancoRepository.findByProtocolos(protocolos));
    }

    private List<MassivasBFFOutputDTO> toDTOs(List<RecuperarTodasAsMassivasProjection> rows) {
        return rows.stream().map(p -> new MassivasBFFOutputDTO(
                        p.getID(),
                        p.getCRIACAO(),
                        p.getFINALIZADO(),
                        p.getSLA(),
                        p.getPROTOCOLO(),
                        p.getEQUIPE(),
                        p.getSTATUS(),
                        p.getTipoSolicitacao(),
                        p.getSOLICITANTE(),
                        p.getRESPONSAVEL(),
                        p.getCATALOGO(),
                        p.getPONTODEACESSO(),
                        p.getDESCRICAO()
                )).toList();
    }
}
