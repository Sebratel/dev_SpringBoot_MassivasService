package br.com.sebratel.massivas.service.massivas;

import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.repository.erp.massivas.RecuperarPrevisaoMassivaPorContratoRepository;
import br.com.sebratel.massivas.repository.erp.projections.PrevisaoMassivaPorContratoProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RecuperarPrevisaoMassivaPorContratoService {

    private final RecuperarPrevisaoMassivaPorContratoRepository recuperarPrevisaoMassivaPorContratoRepository;

    @Autowired
    public RecuperarPrevisaoMassivaPorContratoService(RecuperarPrevisaoMassivaPorContratoRepository recuperarPrevisaoMassivaPorContratoRepository) {
        this.recuperarPrevisaoMassivaPorContratoRepository = recuperarPrevisaoMassivaPorContratoRepository;
    }

    public ImpactedUsersOutputDTO executar(String contractNumber) {
        log.info("Buscando previsão de finalização da massiva no Voalle pelo número do contrato: {}", contractNumber);
        PrevisaoMassivaPorContratoProjection massiva = recuperarPrevisaoMassivaPorContratoRepository
                .findEstimatedEndByContractNumber(contractNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum evento massivo ativo encontrado no Voalle para o contrato: " + contractNumber));

        return montarImpactedUsersDTO(contractNumber, massiva);
    }

    private ImpactedUsersOutputDTO montarImpactedUsersDTO(String contractNumber, PrevisaoMassivaPorContratoProjection massiva) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime previsao = massiva.getPREVISAO();

        long hoursRemaining = ChronoUnit.HOURS.between(now, previsao);
        long estimateTimeOfRestoration = Math.max(0, hoursRemaining);
        long estimatedTimeHour = previsao.getHour();

        String reason = massiva.getDESCRICAO() != null ? massiva.getDESCRICAO()
                : (massiva.getTIPO_SOLICITACAO() != null ? massiva.getTIPO_SOLICITACAO() : "Evento massivo");

        ImpactDetailsOutputDTO impactDetails = ImpactDetailsOutputDTO.builder()
                .reason(reason)
                .estimateTimeOfRestoration(estimateTimeOfRestoration > 0 ? estimateTimeOfRestoration : 2)
                .estimatedTimeHour(estimatedTimeHour)
                .build();

        Map<Long, ImpactDetailsOutputDTO> impactedUser = new HashMap<>();
        impactedUser.put(Long.parseLong(contractNumber), impactDetails);

        return ImpactedUsersOutputDTO.builder()
                .impactedUsers(List.of(impactedUser))
                .build();
    }
}
