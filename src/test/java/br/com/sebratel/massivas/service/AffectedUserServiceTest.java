package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AffectedUserRequestDTO;
import br.com.sebratel.massivas.dto.CreateImpactedUsersInputDTO;
import br.com.sebratel.massivas.dto.massivas.ImpactedUsersOutputDTO;
import br.com.sebratel.massivas.dto.massivas.api.FinalizaRegistroMassivoInputDTO;
import br.com.sebratel.massivas.enums.ClientType;
import br.com.sebratel.massivas.exceptions.DomainException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.model.entity.AffectedUsersEntity;
import br.com.sebratel.massivas.repository.afetados.AffectedUserRepository;
import br.com.sebratel.massivas.service.massivas.FinalizarMassivaNoEllevenApiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffectedUserServiceTest {

    @Mock
    private AffectedUserRepository repository;

    @Mock
    private FinalizarMassivaNoEllevenApiService finalizarMassivaNoEllevenApiService;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private AffectedUserService service;

    @Test
    void getAllShouldReturnMappedDTO() {
        // Arrange
        AffectedUsersEntity user1 = new AffectedUsersEntity();
        user1.setContractId(1L);
        user1.setFinishDate(LocalDateTime.now().plusHours(3));
        user1.setReason("Maintenance");

        AffectedUsersEntity user2 = new AffectedUsersEntity();
        user2.setContractId(2L);
        user2.setFinishDate(LocalDateTime.now().minusHours(1)); // minutesRemaining <= 0
        user2.setReason("Outage");
        
        when(repository.findAll()).thenReturn(List.of(user1, user2));

        // Act
        ImpactedUsersOutputDTO result = service.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getImpactedUsers().size());
        assertEquals(3L, result.getImpactedUsers().get(0).get(1L).getEstimateTimeOfRestoration());
        assertEquals(1L, result.getImpactedUsers().get(1).get(2L).getEstimateTimeOfRestoration()); // Massiva vencida: fallback de 1 hora
    }

    @Test
    void createImpactedUsersDTOShouldSaveAndReturnDTO() {
        // Arrange
        AffectedUserRequestDTO userDTO = AffectedUserRequestDTO.builder()
                .contractId(1L)
                .protocol(123L)
                .finishDate(LocalDateTime.now().plusHours(5))
                .build();
        List<AffectedUserRequestDTO> inputList = List.of(userDTO);
        CreateImpactedUsersInputDTO createImpactedUsersInputDTO = new CreateImpactedUsersInputDTO();
        createImpactedUsersInputDTO.setUsuarioAfetadoEntities(inputList);
        createImpactedUsersInputDTO.setAssignmentId(150L);

        AffectedUsersEntity userEntity = new AffectedUsersEntity();
        userEntity.setContractId(1L);
        userEntity.setFinishDate(userDTO.getFinishDate());
        
        when(employeeService.hasB2BinInput(anyList())).thenReturn(false);
        when(repository.saveAll(anyList())).thenReturn(List.of(userEntity));

        // Act
        ImpactedUsersOutputDTO result = service.createImpactedUsersDTO(createImpactedUsersInputDTO);

        // Assert
        assertNotNull(result);
        verify(repository).saveAll(anyList());
        verify(employeeService).hasB2BinInput(anyList());
    }

    @Test
    void createImpactedUsersDTOShouldSetCorporateClientTypeWhenEmployeeServiceReturnsTrue() {
        // Arrange
        AffectedUserRequestDTO userDTO = AffectedUserRequestDTO.builder()
                .contractId(1L)
                .protocol(123L)
                .finishDate(LocalDateTime.now().plusHours(5))
                .build();
        CreateImpactedUsersInputDTO input = new CreateImpactedUsersInputDTO();
        input.setUsuarioAfetadoEntities(List.of(userDTO));
        input.setAssignmentId(150L);

        when(employeeService.hasB2BinInput(anyList())).thenReturn(true);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.createImpactedUsersDTO(input);

        // Assert
        ArgumentCaptor<List<AffectedUsersEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(ClientType.CORPORATE, captor.getValue().get(0).getClientType());
    }

    @Test
    void createImpactedUsersDTOShouldSetNormalClientTypeWhenEmployeeServiceReturnsFalse() {
        // Arrange
        AffectedUserRequestDTO userDTO = AffectedUserRequestDTO.builder()
                .contractId(1L)
                .protocol(123L)
                .finishDate(LocalDateTime.now().plusHours(5))
                .build();
        CreateImpactedUsersInputDTO input = new CreateImpactedUsersInputDTO();
        input.setUsuarioAfetadoEntities(List.of(userDTO));
        input.setAssignmentId(150L);

        when(employeeService.hasB2BinInput(anyList())).thenReturn(false);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.createImpactedUsersDTO(input);

        // Assert
        ArgumentCaptor<List<AffectedUsersEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(ClientType.NORMAL, captor.getValue().get(0).getClientType());
    }

    @Test
    void createImpactedUsersDTOShouldThrowDomainExceptionWhenSavedListIsEmpty() {
        // Arrange
        AffectedUserRequestDTO userDTO = AffectedUserRequestDTO.builder()
                .contractId(1L)
                .protocol(123L)
                .finishDate(LocalDateTime.now().plusHours(5))
                .build();
        CreateImpactedUsersInputDTO input = new CreateImpactedUsersInputDTO();
        input.setUsuarioAfetadoEntities(List.of(userDTO));
        input.setAssignmentId(150L);

        when(employeeService.hasB2BinInput(anyList())).thenReturn(false);
        when(repository.saveAll(anyList())).thenReturn(List.of());

        // Act & Assert
        assertThrows(DomainException.class, () -> service.createImpactedUsersDTO(input));
    }

    @Test
    void createImpactedUsersDTOShouldCallFinalizeServiceWhenExceptionOccurs() {
        // Arrange
        AffectedUserRequestDTO userDTO = AffectedUserRequestDTO.builder()
                .contractId(1L)
                .protocol(123L)
                .finishDate(LocalDateTime.now().plusHours(5))
                .build();
        CreateImpactedUsersInputDTO input = new CreateImpactedUsersInputDTO();
        input.setUsuarioAfetadoEntities(List.of(userDTO));
        input.setAssignmentId(150L);

        when(employeeService.hasB2BinInput(anyList())).thenReturn(false);
        when(repository.saveAll(anyList())).thenThrow(new RuntimeException("DB Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.createImpactedUsersDTO(input));
        verify(finalizarMassivaNoEllevenApiService).executar(any(FinalizaRegistroMassivoInputDTO.class));
    }

    @Test
    void getUsuariosAfetadosByContractIdShouldReturnUserWhenExists() {
        // Arrange
        Long contractId = 1L;
        AffectedUsersEntity user = new AffectedUsersEntity();
        user.setContractId(contractId);
        user.setFinishDate(LocalDateTime.now().plusHours(3));
        when(repository.findFirstByContractId(contractId)).thenReturn(Optional.of(user));

        // Act
        ImpactedUsersOutputDTO result = service.getUsuariosAfetadosByContractId(contractId);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getUsuariosAfetadosByContractIdShouldThrowExceptionWhenNotFound() {
        // Arrange
        Long contractId = 999L;
        when(repository.findFirstByContractId(contractId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getUsuariosAfetadosByContractId(contractId));
    }

    @Test
    void changeEstimationTimeShouldLogWarnWhenNoRowsAffected() {
        // Arrange
        Long protocol = 123L;
        LocalDateTime finish = LocalDateTime.now().plusDays(1);
        when(repository.findByProtocol(protocol)).thenReturn(List.of(new AffectedUsersEntity()));
        when(repository.updateUsersByProtocol(protocol, finish)).thenReturn(0);

        // Act
        service.changeEstimationTime(protocol, finish);

        // Assert
        verify(repository).updateUsersByProtocol(protocol, finish);
    }

    @Test
    void getUsuariosByProtocolShouldReturnFilteredData() {
        // Arrange
        Long protocol = 123L;
        AffectedUsersEntity user = new AffectedUsersEntity();
        user.setContractId(1L);
        user.setFinishDate(LocalDateTime.now().plusHours(3));
        
        when(repository.findByProtocol(protocol)).thenReturn(List.of(user));

        // Act
        ImpactedUsersOutputDTO result = service.getUsuariosByProtocol(protocol);

        // Assert
        assertEquals(1, result.getImpactedUsers().size());
    }

    @Test
    void getUsuariosAfetadosByPppoeShouldReturnUserWhenExists() {
        // Arrange
        String pppoe = "user@test";
        AffectedUsersEntity user = new AffectedUsersEntity();
        user.setContractId(1L);
        user.setFinishDate(LocalDateTime.now().plusHours(3));
        
        when(repository.findByPppoe(pppoe)).thenReturn(Optional.of(user));

        // Act
        ImpactedUsersOutputDTO result = service.getUsuariosAfetadosByPppoe(pppoe);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getUsuariosAfetadosByPppoeShouldThrowExceptionWhenNotFound() {
        // Arrange
        String pppoe = "notfound@test";
        when(repository.findByPppoe(pppoe)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getUsuariosAfetadosByPppoe(pppoe));
    }

    @Test
    void removeUsersByProtocolShouldCallRepository() {
        // Arrange
        Long protocol = 123L;
        when(repository.deleteByProtocol(protocol)).thenReturn(5);

        // Act
        service.removeUsersByProtocol(protocol);

        // Assert
        verify(repository).deleteByProtocol(protocol);
    }

    @Test
    void changeEstimationTimeShouldUpdateWhenUsersExist() {
        // Arrange
        Long protocol = 123L;
        LocalDateTime finish = LocalDateTime.now().plusDays(1);
        when(repository.findByProtocol(protocol)).thenReturn(List.of(new AffectedUsersEntity()));
        when(repository.updateUsersByProtocol(protocol, finish)).thenReturn(1);

        // Act
        service.changeEstimationTime(protocol, finish);

        // Assert
        verify(repository).updateUsersByProtocol(protocol, finish);
    }

    @Test
    void changeEstimationTimeShouldThrowExceptionWhenNoUsers() {
        // Arrange
        Long protocol = 123L;
        when(repository.findByProtocol(protocol)).thenReturn(List.of());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.changeEstimationTime(protocol, LocalDateTime.now()));
    }
}
