package br.com.sebratel.massivas.repository.afetados;

import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface AffectedUserRepository {
    List<AffectedUsersEntity> saveAll(List<AffectedUsersEntity> affectedUsersEntity);
    Optional<AffectedUsersEntity> findByPppoe(String pppoe);
    List<AffectedUsersEntity> findByProtocol(Long protocol);
    Integer deleteByProtocol(Long protocol);
    List<AffectedUsersEntity> findAll();
    Integer updateUsersByProtocol(Long protocol, LocalDateTime finishDate);
    Optional<AffectedUsersEntity> findFirstByContractId(Long contractId);
}
