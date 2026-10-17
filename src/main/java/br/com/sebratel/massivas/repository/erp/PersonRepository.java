package br.com.sebratel.massivas.repository.erp;

import br.com.sebratel.massivas.model.entity.PersonEntity;
import br.com.sebratel.massivas.repository.erp.projections.ContractProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, Long> {
    Optional<PersonEntity> findByTxId(String cpf);

    /**
     * Um CPF pode ter mais de um contrato, e o join com authentication_contracts pode
     * duplicar linhas do mesmo contrato. Retorna lista para nao estourar
     * IncorrectResultSizeDataAccessException quando ha mais de um resultado.
     */
    @Query(value = """
        SELECT DISTINCT
            ac.contract_id as contractId
        FROM people p
        INNER JOIN contracts c on p.id = c.client_id
        INNER JOIN authentication_contracts ac on ac.contract_id = c.id
        WHERE p.tx_id = :cpf
        ORDER BY ac.contract_id
        """, nativeQuery = true)
    List<ContractProjection> findContractsByCPF(String cpf);
}
