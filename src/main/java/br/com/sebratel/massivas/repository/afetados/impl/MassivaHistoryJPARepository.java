package br.com.sebratel.massivas.repository.afetados.impl;

import br.com.sebratel.massivas.model.entity.MassivaHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MassivaHistoryJPARepository extends JpaRepository<MassivaHistoryEntity, Long> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE massiva_history SET status = 'encerrada', closed_at = :closedAt, " +
            "closed_by = :closedBy, close_description = :closeDescription " +
            "WHERE protocol = :protocol AND status <> 'encerrada'", nativeQuery = true)
    Integer encerrarPorProtocolo(@Param("protocol") Long protocol,
                                 @Param("closedAt") LocalDateTime closedAt,
                                 @Param("closedBy") String closedBy,
                                 @Param("closeDescription") String closeDescription);

    @Modifying
    @Transactional
    @Query(value = "UPDATE massiva_history SET status = 'cancelada', closed_at = :cancelledAt, " +
            "closed_by = :cancelledBy, close_description = :cancelDescription " +
            "WHERE protocol = :protocol AND status <> 'cancelada'", nativeQuery = true)
    Integer cancelarPorProtocolo(@Param("protocol") Long protocol,
                                 @Param("cancelledAt") LocalDateTime cancelledAt,
                                 @Param("cancelledBy") String cancelledBy,
                                 @Param("cancelDescription") String cancelDescription);

    /**
     * Massivas que atingiram os contratos e abriram ou encerraram desde {@code desde}. A ligacao contrato -> massiva
     * vem de massiva_affected_clients, que nao e apagada quando a massiva fecha (diferente de assignment_clients).
     */
    @Query(value = "SELECT h.protocol AS protocolo, h.title AS titulo, h.status AS status, h.opened_at AS abertaEm, " +
            "h.closed_at AS encerradaEm, h.close_description AS descricao, h.source AS origem, " +
            "COUNT(DISTINCT ac.contract_id) AS contratos " +
            "FROM massiva_affected_clients ac " +
            "JOIN massiva_history h ON h.id = ac.massiva_history_id " +
            "WHERE ac.contract_id IN (:contratos) AND (h.opened_at >= :desde OR h.closed_at >= :desde) " +
            "GROUP BY h.id, h.protocol, h.title, h.status, h.opened_at, h.closed_at, h.close_description, h.source " +
            "ORDER BY h.opened_at DESC", nativeQuery = true)
    List<HistoricoClienteProjection> historicoDosContratos(@Param("contratos") Collection<Long> contratos,
                                                           @Param("desde") LocalDateTime desde);
}
