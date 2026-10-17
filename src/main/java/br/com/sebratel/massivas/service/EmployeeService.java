package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.CorporativoOutputDTO;
import br.com.sebratel.massivas.dto.InsigniaOutputDTO;
import br.com.sebratel.massivas.exceptions.InsigniaNotFoundException;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.model.Employee;
import br.com.sebratel.massivas.repository.erp.EmployeeRepository;
import br.com.sebratel.massivas.repository.erp.projections.InsigniaProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Long getPersonIdByEmail(String email) {
        log.info("Buscando PersonId para o email: {}", email);
        // A query já vem ordenada (cadastro com CPF primeiro, depois menor id). Deduplicamos
        // preservando essa ordem — o join pode repetir o mesmo id quando v_users tem a linha
        // duplicada, e nesse caso NÃO é ambiguidade real de pessoa.
        List<Long> ids = employeeRepository.findPersonIdsByEmail(email).stream().distinct().toList();
        if (ids.isEmpty()) {
            throw new ResourceNotFoundException("PersonId não encontrado com o email fornecido: " + email);
        }
        if (ids.size() > 1) {
            // Cadastro duplicado no ERP: o mesmo e-mail tem mais de uma pessoa DISTINTA. Não
            // derrubamos a abertura (evita HTTP 500) — usamos o primeiro da ordem (cadastro com
            // CPF / mais antigo) e registramos o aviso para o duplicado ser tratado no Voalle.
            log.warn("E-mail com mais de um PersonId no ERP (cadastro duplicado): {} -> {}. Usando: {}.",
                    email, ids, ids.getFirst());
        }
        return ids.getFirst();
    }

    /**
     * Resolve nome e e-mail de uma pessoa do ERP. Usado como contingência quando o JWT não traz as claims.
     * Retorna vazio se a pessoa não existe ou não tem nome/e-mail cadastrados.
     */
    public Optional<Employee> findEmployeeByPersonId(Long personId) {
        if (personId == null) {
            return Optional.empty();
        }
        return employeeRepository.findContactByPersonId(personId)
                .filter(c -> c.getName() != null && !c.getName().isBlank()
                        && c.getEmail() != null && !c.getEmail().isBlank())
                .map(c -> new Employee(c.getEmail(), c.getName()));
    }

    public boolean hasB2BinInput(List<Long> list) {
        return employeeRepository.hasB2BinInput(list);
    }

    private static final Set<String> INSIGNIAS_CORPORATIVAS = Set.of("Contrato Corporativo PME", "Contrato Corporativo");

    public CorporativoOutputDTO getCorporativoByTxId(@Valid @NotNull String txId) {
        log.info("Verificando se o cliente é corporativo para o cpf/cnpj: {}", txId);

        if (employeeRepository.findByTxId(txId).isEmpty()) {
            throw new ResourceNotFoundException("Cliente não encontrado para o cpf/cnpj fornecido: " + txId);
        }

        // O mesmo cpf/cnpj pode ter varios registros em people (ex.: matriz e filiais), cada um
        // com sua insignia. A query devolve as insignias distintas; se alguma for corporativa,
        // o cliente e corporativo.
        List<InsigniaProjection> insignias = employeeRepository.findInsigniasByTxId(txId);
        if (insignias.isEmpty()) {
            throw new InsigniaNotFoundException("Cliente encontrado, porém sem insígnia cadastrada para o cpf/cnpj: " + txId);
        }

        InsigniaProjection insigniaProjection = insignias.stream()
                .filter(i -> INSIGNIAS_CORPORATIVAS.contains(i.getTitle()))
                .findFirst()
                .orElse(insignias.getFirst());

        InsigniaOutputDTO insignia = InsigniaOutputDTO.fromProjection(insigniaProjection);
        boolean corporativo = INSIGNIAS_CORPORATIVAS.contains(insignia.getTitle());
        return new CorporativoOutputDTO(insignia, corporativo);
    }

    public String getPersonByCPF(@Valid @NotNull String txId) {
        log.info("Buscando PersonId para o cpf: {}", txId);
        return employeeRepository.findByTxId(txId).getFirst();
    }

    public String getTxIdByContract(@Valid @NotNull Long contract) {
        log.info("Buscando txId para o contrato: {}", contract);
        return employeeRepository.findTxIdByContract(contract);
    }
}
