package br.com.sebratel.massivas.repository.erp.massivas;

import br.com.sebratel.massivas.model.ErpContract;
import br.com.sebratel.massivas.repository.erp.projections.PrevisaoMassivaPorContratoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecuperarPrevisaoMassivaPorContratoRepository extends JpaRepository<ErpContract, Long> {

    @Query(value = """
                SELECT
                    a.id AS ID,
                    c.contract_number AS CONTRATO,
                    ai.protocol AS PROTOCOLO,
                    a.created AS CRIACAO,
                    a.final_date AS PREVISAO,
                    a.conclusion_date AS FINALIZADO,
                    t2.title AS STATUS,
                    it.title AS TIPO_SOLICITACAO,
                    a.description AS DESCRICAO
                FROM assignments a
                    INNER JOIN assignment_incidents ai ON ai.assignment_id = a.id
                    INNER JOIN contract_service_tags cst ON cst.id = ai.contract_service_tag_id
                    INNER JOIN contracts c ON c.id = cst.contract_id
                    LEFT JOIN incident_status t2 ON t2.id = ai.incident_status_id
                    LEFT JOIN incident_types it ON it.id = ai.incident_type_id
                WHERE c.contract_number = :contractNumber
                  AND it.id IN (1176, 302, 1257, 1265)
                  AND t2.title NOT IN ('Encerrado', 'Cancelado')
                ORDER BY a.created DESC
                LIMIT 1
            """, nativeQuery = true)
    Optional<PrevisaoMassivaPorContratoProjection> findEstimatedEndByContractNumber(@Param("contractNumber") String contractNumber);
}
