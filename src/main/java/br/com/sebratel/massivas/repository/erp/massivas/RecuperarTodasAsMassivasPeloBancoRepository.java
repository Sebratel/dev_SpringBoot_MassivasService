package br.com.sebratel.massivas.repository.erp.massivas;

import br.com.sebratel.massivas.model.ErpContract;
import br.com.sebratel.massivas.repository.erp.projections.RecuperarTodasAsMassivasProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecuperarTodasAsMassivasPeloBancoRepository extends JpaRepository<ErpContract, Long> {

    @Query(value = """
                SELECT
                    a.id AS ID,
                    a.created AS CRIACAO,
                    a.final_date AS SLA,
                    a.conclusion_date AS FINALIZADO,
                    ai.protocol AS PROTOCOLO,
                    a.description AS DESCRICAO,
                    t.title AS EQUIPE,
                    t2.title AS STATUS,
                    it.title AS "tipoSolicitacao",
                    v.name AS SOLICITANTE,
                    p2.name AS RESPONSAVEL,
                    cs.title AS CATALOGO,
                    aap.title AS PONTODEACESSO
                FROM assignments a
                    INNER JOIN assignment_incidents ai ON ai.assignment_id = a.id
                    LEFT JOIN teams t ON t.id = ai.team_id
                    LEFT JOIN incident_status t2 ON t2.id = ai.incident_status_id
                    LEFT JOIN incident_types it ON it.id = ai.incident_type_id
                    LEFT JOIN v_users v ON v.id = a.created_by
                    LEFT JOIN people p2 ON p2.id = a.requestor_id
                    LEFT JOIN catalog_services cs ON cs.id = ai.catalog_service_id
                    LEFT JOIN authentication_access_points aap ON aap.id = ai.authentication_access_point_id
                WHERE it.id IN (1176, 302, 1257, 1265)
                  AND (
                        t2.title NOT IN ('Encerrado', 'Cancelado')
                        OR
                        (t2.title IN ('Encerrado', 'Cancelado') AND a.conclusion_date >= CURRENT_DATE - INTERVAL '1 month')
                      )
                ORDER BY a.id DESC
            """, nativeQuery = true)
    List<RecuperarTodasAsMassivasProjection> findActiveAssignments();

    /**
     * Status atual no Elleven para protocolos ESPECIFICOS, sem filtro de tipo de incidente nem
     * janela de data. Usado pela reconciliacao Splitters -> Voalle: o Splitters ja nos diz quais
     * protocolos estao 'aberta' e queremos saber se o Elleven ja encerrou/cancelou, nao importa
     * o incident_type. Evita repetir o erro do filtro fixo de tipos em findActiveAssignments
     * (que ja deixou passar batido um tipo de massiva valido - ver historico do PR).
     */
    @Query(value = """
                SELECT
                    a.id AS ID,
                    a.created AS CRIACAO,
                    a.final_date AS SLA,
                    a.conclusion_date AS FINALIZADO,
                    ai.protocol AS PROTOCOLO,
                    a.description AS DESCRICAO,
                    t.title AS EQUIPE,
                    t2.title AS STATUS,
                    it.title AS "tipoSolicitacao",
                    v.name AS SOLICITANTE,
                    p2.name AS RESPONSAVEL,
                    cs.title AS CATALOGO,
                    aap.title AS PONTODEACESSO
                FROM assignments a
                    INNER JOIN assignment_incidents ai ON ai.assignment_id = a.id
                    LEFT JOIN teams t ON t.id = ai.team_id
                    LEFT JOIN incident_status t2 ON t2.id = ai.incident_status_id
                    LEFT JOIN incident_types it ON it.id = ai.incident_type_id
                    LEFT JOIN v_users v ON v.id = a.created_by
                    LEFT JOIN people p2 ON p2.id = a.requestor_id
                    LEFT JOIN catalog_services cs ON cs.id = ai.catalog_service_id
                    LEFT JOIN authentication_access_points aap ON aap.id = ai.authentication_access_point_id
                WHERE ai.protocol IN (:protocolos)
                ORDER BY a.id DESC
            """, nativeQuery = true)
    List<RecuperarTodasAsMassivasProjection> findByProtocolos(@Param("protocolos") List<Long> protocolos);
}