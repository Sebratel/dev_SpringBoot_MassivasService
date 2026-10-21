package br.com.sebratel.massivas.repository.afetados.impl;

import br.com.sebratel.massivas.model.entity.MassivaHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
}
