package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.CorporativoOutputDTO;
import br.com.sebratel.massivas.exceptions.InsigniaNotFoundException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.repository.erp.EmployeeRepository;
import br.com.sebratel.massivas.repository.erp.projections.InsigniaProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    @Test
    void getPersonIdByEmailShouldReturnIdWhenEmailExists() {
        // Arrange
        String email = "test@sebratel.com.br";
        Long expectedId = 123L;
        when(repository.findPersonIdsByEmail(email)).thenReturn(List.of(expectedId));

        // Act
        Long result = service.getPersonIdByEmail(email);

        // Assert
        assertEquals(expectedId, result);
        verify(repository, times(1)).findPersonIdsByEmail(email);
    }

    @Test
    void getPersonIdByEmailShouldReturnFirstOrderedWhenEmailHasDuplicatePeople() {
        // Arrange — caso real que causava o HTTP 500: o mesmo e-mail em duas pessoas.
        // A query devolve o cadastro com CPF primeiro (89822) e o duplicado vazio depois (302328);
        // o service usa o primeiro sem estourar NonUniqueResultException.
        String email = "bruno.soares@sebratel.com.br";
        when(repository.findPersonIdsByEmail(email)).thenReturn(List.of(89822L, 302328L));

        // Act
        Long result = service.getPersonIdByEmail(email);

        // Assert
        assertEquals(89822L, result);
        verify(repository, times(1)).findPersonIdsByEmail(email);
    }

    @Test
    void getPersonIdByEmailShouldCollapseRepeatedSameIdWhenVUsersHasDuplicateRow() {
        // Arrange — v_users com a linha do e-mail duplicada faz o join repetir o MESMO id.
        // Não é ambiguidade real de pessoa: deduplicamos e devolvemos o único id.
        String email = "test@sebratel.com.br";
        when(repository.findPersonIdsByEmail(email)).thenReturn(List.of(89822L, 89822L));

        // Act
        Long result = service.getPersonIdByEmail(email);

        // Assert
        assertEquals(89822L, result);
        verify(repository, times(1)).findPersonIdsByEmail(email);
    }

    @Test
    void getPersonIdByEmailShouldThrowExceptionWhenEmailDoesNotExist() {
        // Arrange
        String email = "notfound@sebratel.com.br";
        when(repository.findPersonIdsByEmail(email)).thenReturn(List.of());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getPersonIdByEmail(email));
        verify(repository, times(1)).findPersonIdsByEmail(email);
    }

    @Test
    void hasB2BinInputShouldReturnRepositoryResponse() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        when(repository.hasB2BinInput(ids)).thenReturn(true);

        // Act
        boolean result = service.hasB2BinInput(ids);

        // Assert
        assertTrue(result);
        verify(repository, times(1)).hasB2BinInput(ids);
    }

    private static InsigniaProjection insignia(long id, String code, String title) {
        InsigniaProjection p = mock(InsigniaProjection.class);
        lenient().when(p.getId()).thenReturn(id);
        lenient().when(p.getCode()).thenReturn(code);
        lenient().when(p.getTitle()).thenReturn(title);
        return p;
    }

    @Test
    void getCorporativoByTxIdShouldNotFailWhenSameTxIdHasSeveralPeopleWithDifferentInsignias() {
        // Regressao: o mesmo cnpj tinha 6 linhas em people e a query de insignia (Optional)
        // lancava IncorrectResultSizeDataAccessException -> HTTP 500.
        String txId = "12345678000190";
        when(repository.findByTxId(txId)).thenReturn(List.of("Matriz", "Filial 1", "Filial 2", "Filial 3", "Filial 4", "Filial 5"));
        List<InsigniaProjection> insignias = List.of(
                insignia(1L, "CORP", "Contrato Corporativo"),
                insignia(2L, "CORP_PME", "Contrato Corporativo PME"),
                insignia(3L, "RES", "Cliente Residencial"));
        when(repository.findInsigniasByTxId(txId)).thenReturn(insignias);

        CorporativoOutputDTO result = service.getCorporativoByTxId(txId);

        assertTrue(result.isCorporativo());
        assertEquals("Contrato Corporativo", result.getInsignia().getTitle());
    }

    @Test
    void getCorporativoByTxIdShouldPreferCorporateInsigniaEvenWhenItIsNotTheFirst() {
        String txId = "11122233000144";
        when(repository.findByTxId(txId)).thenReturn(List.of("Cliente"));
        List<InsigniaProjection> insignias = List.of(
                insignia(3L, "RES", "Cliente Residencial"),
                insignia(2L, "CORP_PME", "Contrato Corporativo PME"));
        when(repository.findInsigniasByTxId(txId)).thenReturn(insignias);

        CorporativoOutputDTO result = service.getCorporativoByTxId(txId);

        assertTrue(result.isCorporativo());
        assertEquals("Contrato Corporativo PME", result.getInsignia().getTitle());
    }

    @Test
    void getCorporativoByTxIdShouldReturnNotCorporateWhenNoInsigniaIsCorporate() {
        String txId = "11122233344";
        when(repository.findByTxId(txId)).thenReturn(List.of("Cliente"));
        List<InsigniaProjection> insignias = List.of(insignia(3L, "RES", "Cliente Residencial"));
        when(repository.findInsigniasByTxId(txId)).thenReturn(insignias);

        CorporativoOutputDTO result = service.getCorporativoByTxId(txId);

        assertFalse(result.isCorporativo());
        assertEquals("Cliente Residencial", result.getInsignia().getTitle());
    }

    @Test
    void getCorporativoByTxIdShouldThrowInsigniaNotFoundWhenPersonHasNoInsignia() {
        String txId = "55566677788";
        when(repository.findByTxId(txId)).thenReturn(List.of("Cliente"));
        when(repository.findInsigniasByTxId(txId)).thenReturn(List.of());

        assertThrows(InsigniaNotFoundException.class, () -> service.getCorporativoByTxId(txId));
    }

    @Test
    void getCorporativoByTxIdShouldThrowResourceNotFoundWhenPersonDoesNotExist() {
        String txId = "00000000000";
        when(repository.findByTxId(txId)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.getCorporativoByTxId(txId));
        verify(repository, never()).findInsigniasByTxId(txId);
    }
}
