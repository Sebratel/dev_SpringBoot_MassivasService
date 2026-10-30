package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.CreateImpactedUsersInputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.enums.ClientType;
import br.com.sebratel.massivas.exceptions.DomainException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import br.com.sebratel.massivas.repository.afetados.AffectedUserRepository;
import br.com.sebratel.massivas.service.massivas.FinalizarMassivaNoEllevenApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AffectedUserService {
    private final AffectedUserRepository affectedUserRepository;
    private final FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService;
    private final EmployeeService employeeService;

    @Autowired
    public AffectedUserService(AffectedUserRepository affectedUserRepository, FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService, EmployeeService employeeService) {
        this.affectedUserRepository = affectedUserRepository;
        this.finalizarMassivaNoEllevenApiService = finalizarMassivaNoEllevenApiService;
        this.employeeService =  employeeService;
    }

    @Transactional(transactionManager = "afetadosTransactionManager")
    public ImpactedUsersOutputDTO createImpactedUsersDTO(CreateImpactedUsersInputDTO input) {
        log.info("Salvando lista de {} usuários afetados", input.getUsuarioAfetadoEntities().size());
        List<AffectedUsersEntity> usuarioAfetadoEntities;
        List<AffectedUsersEntity> affectedUsersContractList = input.getUsuarioAfetadoEntities().stream().map(dto -> {
            AffectedUsersEntity entity = new AffectedUsersEntity();
            entity.setPppoe(dto.getPppoe());
            entity.setProtocol(dto.getProtocol());
            entity.setReason(dto.getReason());
            entity.setContractId(dto.getContractId());
            entity.setFinishDate(dto.getFinishDate());
            entity.setCreated(dto.getCreated());
            entity.setCreatedBy(dto.getCreatedBy());

            List<Long> affectedUserContractList = List.of(dto.getContractId());
            entity.setClientType(employeeService.hasB2BinInput(affectedUserContractList) ? ClientType.CORPORATE : ClientType.NORMAL);
            return entity;
        }).toList();

        try {
            usuarioAfetadoEntities = affectedUserRepository.saveAll(affectedUsersContractList);
            if(usuarioAfetadoEntities.isEmpty()) {
                throw new DomainException("Não é aceito criar uma massiva sem clientes afetados. Verificar chamada");
            }
        } catch (Exception e) {
            LocalDateTime now = LocalDateTime.now();
            log.error("Erro ao salvar usuários afetados às {}. Chamando finalização massiva no Elleven.", now, e);
            FinalizaRegistroMassivoInputDTO finalizarInput = FinalizaRegistroMassivoInputDTO
                    .builder()
                    .assignmentId(input.getAssignmentId().toString())
                    .incidentStatusId("" + 8)
                    .description("O protocolo teve que ser encerrado pois houve erro de comunicação entre os serviços voalle e splitters. Por favor entre em contato com o desenvolvimento assim que possível com a data " + now)
                    .progress("0")
                    .priority("35")
                    .notificationTarget("0")
                    .privateReport("true")
                    .build();
            finalizarMassivaNoEllevenApiService.executar(finalizarInput);
            throw e;
        }
        log.info("Usuários afetados para o protocolo criados com sucesso.");
        return getImpactedUsersDTO(usuarioAfetadoEntities);
    }

    private ImpactedUsersOutputDTO getImpactedUsersDTO(List<AffectedUsersEntity> affectedUsers) {
        log.debug("Criando DTO de usuários afetados.{}", affectedUsers);
        List<Map<Long, ImpactDetailsOutputDTO>> impactedUsersDTO = affectedUsers.stream().map(affectedUser -> {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime finish = affectedUser.getFinishDate();
            // Arredonda para cima: informar 1h para uma massiva que ainda tem 1h50 restantes
            // subestima a previsao passada ao cliente.
            long minutesRemaining = Duration.between(now, finish).toMinutes();
            long estimateTimeOfRestoration = (minutesRemaining <= 0)
                    ? 1
                    : (long) Math.ceil(minutesRemaining / 60.0);
            long timeHour = affectedUser.getFinishDate().getHour();

            ImpactDetailsOutputDTO impactDetailsDTO = ImpactDetailsOutputDTO.builder()
                    .reason(affectedUser.getReason())
                    .estimateTimeOfRestoration(estimateTimeOfRestoration)
                    .estimatedTimeHour(timeHour)
                    .estimatedFinish(finish)
                    .build();
            Map<Long, ImpactDetailsOutputDTO> impactedUsers = new HashMap<>();
            impactedUsers.put(affectedUser.getContractId(), impactDetailsDTO);
            return impactedUsers;
        }).toList();

        return ImpactedUsersOutputDTO.builder()
                .impactedUsers(impactedUsersDTO)
                .build();
    }

    public ImpactedUsersOutputDTO getAll() {
        log.info("Buscando usuários afetados");
        return getImpactedUsersDTO(affectedUserRepository.findAll());
    }

    public ImpactedUsersOutputDTO getUsuariosByProtocol(Long protocol) {
        log.info("Buscando usuários afetados pelo protocolo: {}", protocol);
        return getImpactedUsersDTO(affectedUserRepository.findByProtocol(protocol));
    }

    public ImpactedUsersOutputDTO getUsuariosAfetadosByPppoe(String pppoe) {
        log.info("Buscando usuário afetado pelo PPPoE: {}", pppoe);
        AffectedUsersEntity affectedUsersEntity = affectedUserRepository.findByPppoe(pppoe).orElseThrow(() -> new ResourceNotFoundException("Usuário afetado não encontrado"));
        log.info("Usuário afetado encontrado: {}", affectedUsersEntity);
        return getImpactedUsersDTO(List.of(affectedUsersEntity));
    }

    public void removeUsersByProtocol(Long protocol) {
        log.info("Removendo usuários do protocolo: {}", protocol);
        Integer numeroDeLinhasAfetadas = affectedUserRepository.deleteByProtocol(protocol);
        log.info("{} linhas foram afetadas na deleção dos usuarios de protocolo {}", numeroDeLinhasAfetadas, protocol);
    }

    @Transactional(transactionManager = "afetadosTransactionManager")
    public void changeEstimationTime(Long protocol, LocalDateTime finishDate) {
        log.info("Alterando data estimada para finalização");
        List<AffectedUsersEntity> affectedUsers = affectedUserRepository.findByProtocol(protocol);
        if (affectedUsers.isEmpty()) {
            log.warn("Nenhum usuário afetado encontrado para o protocolo: {}", protocol);
            throw new ResourceNotFoundException("Nenhum usuário afetado encontrado para o protocolo: " + protocol);
        }
        log.info("Foram encontrados {} usuarios no protocolo {}", affectedUsers.size(), protocol);
        Integer numberOfAffecteds = affectedUserRepository.updateUsersByProtocol(protocol, finishDate);
        log.info("Data de finalização alterada para {} nos usuarios de protocolo {}", finishDate, protocol);

        if (numberOfAffecteds == 0) {
            log.warn("Nenhuma linha afetada para o protocolo {} ", protocol);
        }
    }

    public ImpactedUsersOutputDTO getUsuariosAfetadosByContractId(Long contractId) {
        log.info("Buscando usuário afetado pelo contractId: {}", contractId);
        AffectedUsersEntity affectedUsersEntity = affectedUserRepository.findFirstByContractId(contractId).orElseThrow(() -> new ResourceNotFoundException("Usuário afetado não encontrado"));
        log.info("Usuário afetado encontrado por contractId: {}", affectedUsersEntity);
        return getImpactedUsersDTO(List.of(affectedUsersEntity));
    }
}
