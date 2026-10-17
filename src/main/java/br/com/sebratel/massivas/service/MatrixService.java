package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.MatrixMassiveOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.model.entity.PersonEntity;
import br.com.sebratel.massivas.repository.erp.PersonRepository;
import br.com.sebratel.massivas.repository.erp.projections.ContractProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class MatrixService {

    final PersonRepository personRepository;
    final AffectedUserService affectedUserService;

    public MatrixService(PersonRepository personRepository, AffectedUserService affectedUserService) {
        this.personRepository = personRepository;
        this.affectedUserService = affectedUserService;
    }

    public MatrixMassiveOutputDTO getContractInfoByCPF(String cpf) {

        try {
            log.info("Iniciando busca de contrato para o CPF: {}", cpf);
            Optional<PersonEntity> personEntity = personRepository.findByTxId(cpf);

            if (personEntity.isEmpty()) {
                log.warn("Nenhum person encontrado: {}", cpf);
                return notFoundClient();
            }

            log.info("Encontrado personId {}, iniciando procura de contrato pelo CPF", personEntity.get().getId());

            List<ContractProjection> contracts = personRepository.findContractsByCPF(cpf);

            if (contracts.isEmpty()) {
                log.info("Cliente de CPF {} nao tem contrato vinculado", cpf);
                return notFoundClient();
            }

            log.info("Encontrados {} contratos para o CPF, verificando massiva em cada um", contracts.size());

            for (ContractProjection contract : contracts) {
                Optional<ImpactDetailsOutputDTO> impactDetails = findImpactByContract(contract.getContractId());

                if (impactDetails.isEmpty()) {
                    continue;
                }

                ImpactDetailsOutputDTO impact = impactDetails.get();
                log.info("Massiva encontrada para o contrato {}", contract.getContractId());

                return MatrixMassiveOutputDTO
                        .builder()
                        .resolutionTime(impact.getEstimateTimeOfRestoration().intValue())
                        .resolutionTimeHour("" + impact.getEstimatedTimeHour())
                        .authenticationProblems(1L)
                        .status("client_found")
                        .build();
            }

            log.info("Nenhum dos {} contratos do CPF possui massiva", contracts.size());
            return notFoundClient();
        }catch (Exception e) {
            log.info("Não foi possível localizar contrato para o CPF {}: {}", cpf, e.getMessage());
            return notFoundClient();
        }
    }

    /**
     * Consulta a massiva de um contrato reaproveitando o mesmo service que atende
     * GET /api/v1/afetados/contract/{contractId}, para que os dois endpoints respondam
     * a partir da mesma fonte.
     *
     * O ResourceNotFoundException e tratado aqui, e nao no catch externo, porque um
     * contrato sem massiva nao pode interromper a varredura dos contratos seguintes.
     */
    private Optional<ImpactDetailsOutputDTO> findImpactByContract(Long contractId) {
        try {
            ImpactedUsersOutputDTO impactedUsers = affectedUserService.getUsuariosAfetadosByContractId(contractId);
            return impactedUsers.getImpactedUsers().stream()
                    .map(entry -> entry.get(contractId))
                    .filter(Objects::nonNull)
                    .findFirst();
        } catch (ResourceNotFoundException e) {
            log.info("Nenhum usuario afetado para o contrato {}", contractId);
            return Optional.empty();
        }
    }

    private MatrixMassiveOutputDTO notFoundClient() {
        return MatrixMassiveOutputDTO
                .builder()
                .status("not_found_client")
                .authenticationProblems(0L)
                .resolutionTimeHour("23")
                .build();
    }


}
