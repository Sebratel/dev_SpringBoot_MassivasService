package br.com.sebratel.massivas.repository.afetados.impl;

import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import br.com.sebratel.massivas.repository.afetados.AffectedUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AffectedUserRepositoryImpl implements AffectedUserRepository {

    private final UsuarioAfetadoJPARepository jpaRepository;

    @Autowired
    public AffectedUserRepositoryImpl(UsuarioAfetadoJPARepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<AffectedUsersEntity> saveAll(List<AffectedUsersEntity> affectedUsersEntity) {
        return jpaRepository.saveAll(affectedUsersEntity);
    }

    @Override
    public Optional<AffectedUsersEntity> findByPppoe(String pppoe) {
        return jpaRepository.findByPppoe(pppoe);
    }

    @Override
    public List<AffectedUsersEntity> findByProtocol(Long protocol) {
        return jpaRepository.findByProtocol(protocol);
    }

    @Override
    public Integer deleteByProtocol(Long protocol) {
        return jpaRepository.deleteByProtocol(protocol);
    }

    @Override
    public List<AffectedUsersEntity> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Integer updateUsersByProtocol(Long protocol, LocalDateTime finishDate) {
        return jpaRepository.updateUsersByProtocol(protocol, finishDate);
    }

    @Override
    public Optional<AffectedUsersEntity> findFirstByContractId(Long contractId) {
        return jpaRepository.findFirstByContractId(contractId);
    }
}
