package br.com.sebratel.massivas.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Situacao dos contratos dos clientes no ERP: bloqueio, suspensao e cancelamento. Parte de erp.contracts (todos os
 * contratos do cliente, com ou sem autenticacao: o contrato cancelado perde a conexao). O inicio do estado atual vem
 * da linha aberta de contract_changed_status (contract_blocked_days.number_of_days nao serve: vale 0).
 */
@Repository
public class SituacaoContratoRepository {

    /** Valores de erp.contracts.v_status que interessam ao atendente (sem acento). */
    static final List<String> STATUS = List.of("Bloqueio Financeiro", "Bloqueio Administrativo", "Suspenso", "Cancelado");

    @PersistenceContext
    private EntityManager entityManager;

    public record Linha(long contratoId, String status, LocalDate cancelamento, LocalDateTime inicio, String motivo) {
    }

    /** Quantos contratos (nao excluidos) os clientes tem no ERP. */
    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    public int contarContratos(Collection<Long> clienteIds) {
        Number n = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM erp.contracts c WHERE c.client_id IN (:ids) AND c.deleted IS NOT TRUE")
                .setParameter("ids", clienteIds)
                .getSingleResult();
        return n.intValue();
    }

    /** Contratos em bloqueio, suspensao ou cancelamento (o motivo e o do ultimo evento de cancelamento). */
    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public List<Linha> problemasDosClientes(Collection<Long> clienteIds) {
        List<Object[]> linhas = entityManager.createNativeQuery("""
                        SELECT c.id, c.v_status, c.cancellation_date, ccs.begin_date, mot.title
                        FROM erp.contracts c
                        LEFT JOIN erp.contract_changed_status ccs
                               ON ccs.contract_id = c.id AND ccs.end_date IS NULL AND ccs.deleted IS NOT TRUE
                        LEFT JOIN LATERAL (
                            SELECT cet.title
                            FROM erp.contract_events ce
                            JOIN erp.contract_event_types cet ON cet.id = ce.contract_event_type_id
                            WHERE ce.contract_id = c.id AND ce.deleted IS NOT TRUE AND cet.title LIKE 'Cancelamento%'
                            ORDER BY ce.date DESC, ce.id DESC
                            LIMIT 1) mot ON c.v_status = 'Cancelado'
                        WHERE c.client_id IN (:ids) AND c.deleted IS NOT TRUE AND c.v_status IN (:status)
                        ORDER BY c.id
                        """)
                .setParameter("ids", clienteIds)
                .setParameter("status", STATUS)
                .getResultList();
        List<Linha> resultado = new ArrayList<>(linhas.size());
        for (Object[] l : linhas) {
            resultado.add(new Linha(((Number) l[0]).longValue(), (String) l[1], data(l[2]), dataHora(l[3]),
                    (String) l[4]));
        }
        return resultado;
    }

    private static LocalDate data(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    private static LocalDateTime dataHora(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        return LocalDateTime.parse(o.toString().replace(' ', 'T'));
    }
}
