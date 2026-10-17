package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.MassivasBFFOutputDTO;
import br.com.sebratel.massivas.repository.erp.massivas.RecuperarTodasAsMassivasPeloBancoRepository;
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
        return this.recuperarTodasAsMassivasPeloBancoRepository.findActiveAssignments()
                .stream().map(p -> new MassivasBFFOutputDTO(
                        p.getID(),
                        p.getCRIACAO(),
                        p.getFINALIZADO(),
                        p.getSLA(),
                        p.getPROTOCOLO(),
                        p.getEQUIPE(),
                        p.getSTATUS(),
                        p.getTIPO_SOLICITACAO(),
                        p.getSOLICITANTE(),
                        p.getRESPONSAVEL(),
                        p.getCATALOGO(),
                        p.getPONTODEACESSO(),
                        p.getDESCRICAO()
                )).toList();
    }
}
