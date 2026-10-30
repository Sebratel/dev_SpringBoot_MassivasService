package br.com.sebratel.massivas.repository.erp;

import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Contratos com autenticacao de quem tem o CPF/CNPJ (erp.people.tx_id, gravado so com digitos). */
@Repository
public class ClientePorDocumentoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public List<ClienteContrato> buscarPorDocumento(String digitos) {
        List<Object[]> linhas = entityManager.createNativeQuery("""
                        SELECT DISTINCT p.id AS cliente_id, ac.contract_id AS contrato_id
                        FROM erp.people p
                        INNER JOIN erp.contracts c ON c.client_id = p.id
                        INNER JOIN erp.authentication_contracts ac ON ac.contract_id = c.id
                        WHERE p.tx_id::text = :documento
                        ORDER BY p.id, ac.contract_id
                        """)
                .setParameter("documento", digitos)
                .getResultList();
        List<ClienteContrato> resultado = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            resultado.add(new ClienteContrato(((Number) linha[0]).longValue(), ((Number) linha[1]).longValue()));
        }
        return resultado;
    }
}
