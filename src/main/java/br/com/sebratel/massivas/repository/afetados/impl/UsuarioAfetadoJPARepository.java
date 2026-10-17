package br.com.sebratel.massivas.repository.afetados.impl;

import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UsuarioAfetadoJPARepository extends JpaRepository<AffectedUsersEntity, Long> {
    Optional<AffectedUsersEntity> findByPppoe(String pppoe);
    List<AffectedUsersEntity> findByProtocol(Long protocol);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM assignment_clients WHERE protocol_id = :protocol", nativeQuery = true)
    Integer deleteByProtocol(Long protocol);

    @Modifying
    @Transactional
    @Query(value = "UPDATE assignment_clients SET finish_date = :finishDate  WHERE protocol_id = :protocol", nativeQuery = true)
    Integer updateUsersByProtocol(@Param("protocol") Long protocol, @Param("finishDate") LocalDateTime finishDate);

    Optional<AffectedUsersEntity> findByContractId(Long contractId);

    Optional<AffectedUsersEntity> findFirstByContractId(Long contractId);
}
