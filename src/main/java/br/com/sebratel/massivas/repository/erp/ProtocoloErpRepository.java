package br.com.sebratel.massivas.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Protocolos (assignment_incidents) em aberto dos clientes. "Aberto" e tudo o que nao esta num status terminal:
 * Encerrado (4), Cancelado (8), Fechado (3), Encaminhado para Negativacao (50), Faturado (36) e A Faturar (35).
 * Liga o protocolo ao cliente pelo cliente do protocolo OU pelo contrato da tag de servico, sem repetir.
 */
@Repository
public class ProtocoloErpRepository {

    static final List<Long> STATUS_TERMINAIS = List.of(4L, 8L, 3L, 50L, 36L, 35L);
    /** Teto de linhas lidas por consulta; o servico mostra so os mais recentes. */
    static final int LIMITE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    public record Linha(long protocolo, long assignmentId, String tipo, String situacao, LocalDateTime criado) {
    }

    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public List<Linha> abertosDosClientes(Collection<Long> clienteIds) {
        List<Object[]> linhas = entityManager.createNativeQuery("""
                        SELECT x.protocol, x.assignment_id, it.title, ist.title, a.created
                        FROM (
                            SELECT ai.protocol, ai.assignment_id, ai.incident_type_id, ai.incident_status_id
                            FROM erp.assignment_incidents ai
                            WHERE ai.client_id IN (:ids) AND ai.incident_status_id NOT IN (:terminais)
                            UNION
                            SELECT ai.protocol, ai.assignment_id, ai.incident_type_id, ai.incident_status_id
                            FROM erp.assignment_incidents ai
                            WHERE ai.incident_status_id NOT IN (:terminais)
                              AND ai.contract_service_tag_id IN (
                                  SELECT cst.id FROM erp.contract_service_tags cst
                                  JOIN erp.contracts c ON c.id = cst.contract_id
                                  WHERE c.client_id IN (:ids))
                        ) x
                        JOIN erp.assignments a ON a.id = x.assignment_id AND a.deleted IS NOT TRUE
                        JOIN erp.incident_types it ON it.id = x.incident_type_id
                        JOIN erp.incident_status ist ON ist.id = x.incident_status_id
                        ORDER BY a.created DESC, x.protocol DESC
                        LIMIT %d
                        """.formatted(LIMITE))
                .setParameter("ids", clienteIds)
                .setParameter("terminais", STATUS_TERMINAIS)
                .getResultList();
        List<Linha> resultado = new ArrayList<>(linhas.size());
        for (Object[] l : linhas) {
            resultado.add(new Linha(((Number) l[0]).longValue(), ((Number) l[1]).longValue(), (String) l[2],
                    (String) l[3], l[4] == null ? null : ((Timestamp) l[4]).toLocalDateTime()));
        }
        return resultado;
    }

    /** Proxima visita agendada (nao realizada, daqui para frente) por assignment, so onde o ERP a registrou. */
    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public Map<Long, LocalDateTime> proximasVisitas(Collection<Long> assignmentIds) {
        Map<Long, LocalDateTime> visitas = new HashMap<>();
        if (assignmentIds.isEmpty()) {
            return visitas;
        }
        List<Object[]> linhas = entityManager.createNativeQuery("""
                        SELECT s.assignment_id, MIN(s.start_date)
                        FROM erp.schedules s
                        WHERE s.assignment_id IN (:ids) AND s.performed IS NOT TRUE AND s.start_date >= now()
                        GROUP BY s.assignment_id
                        """)
                .setParameter("ids", assignmentIds)
                .getResultList();
        for (Object[] l : linhas) {
            visitas.put(((Number) l[0]).longValue(), ((Timestamp) l[1]).toLocalDateTime());
        }
        return visitas;
    }
}
