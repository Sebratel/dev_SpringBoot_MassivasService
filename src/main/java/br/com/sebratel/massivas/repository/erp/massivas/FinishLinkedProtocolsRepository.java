package br.com.sebratel.massivas.repository.erp.massivas;

import br.com.sebratel.massivas.model.ErpContract;
import br.com.sebratel.massivas.repository.erp.projections.FinishLinkedProtocolsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinishLinkedProtocolsRepository extends JpaRepository<ErpContract, Long> {

    @Query(value = """
            SELECT
                ai2.assignment_id AS "assignmentLinkado",
                ai.protocol AS "protocoloDaMassiva",
                ai2.protocol AS "protolocoLinkado",
                a.title AS "tituloDaSolicitacao"
            FROM assignment_links al
            INNER JOIN assignment_incidents ai ON ai.assignment_id = al.assignment_id
            INNER JOIN assignment_incidents ai2 ON ai2.assignment_id = al.assignment_linked_id
            INNER JOIN assignments a ON a.id = ai2.assignment_id
            INNER JOIN incident_status s ON s.id = ai2.incident_status_id
            WHERE ai.assignment_id = CAST(:assignmentId AS bigint)
              AND ai2.assignment_id <> CAST(:assignmentId AS bigint)
              AND s.title NOT IN ('Encerrado', 'Cancelado')
            """, nativeQuery = true)
    List<FinishLinkedProtocolsProjection> findLinkedProtocols(@Param("assignmentId") String assignmentId);
}
