package br.com.sebratel.massivas.repository.erp;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Localiza os contratos de quem tem o telefone cadastrado no ERP (erp.people).
 *
 * <p>As colunas de telefone vem de {@code matrix.telefone.colunas} porque o schema do ERP nao esta no repositorio.
 * Os nomes entram no SQL (nao ha bind para identificador), entao so passam se forem identificadores simples.
 */
@Repository
public class ClientePorTelefoneRepository {

    private static final Pattern IDENTIFICADOR = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    @PersistenceContext
    private EntityManager entityManager;

    private final List<String> colunas;

    public ClientePorTelefoneRepository(
            @Value("${matrix.telefone.colunas:phone,cell_phone_1,cell_phone_2}") List<String> colunas) {
        List<String> validas = colunas.stream().map(String::trim).filter(c -> !c.isEmpty()).toList();
        if (validas.isEmpty()) {
            throw new IllegalStateException("matrix.telefone.colunas nao pode ser vazio");
        }
        validas.forEach(c -> {
            if (!IDENTIFICADOR.matcher(c).matches()) {
                throw new IllegalStateException("matrix.telefone.colunas com identificador invalido: " + c);
            }
        });
        this.colunas = validas;
    }

    /** Par cliente (erp.people.id) e contrato com autenticacao, para todos os clientes que usam o telefone. */
    public record ClienteContrato(Long clienteId, Long contratoId) {
    }

    /** Telefones, como cadastrados, de todas as colunas de telefone dos clientes (erp.people.id). */
    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public List<String> telefonesDosClientes(Collection<Long> clienteIds) {
        List<String> brutos = new ArrayList<>();
        for (String coluna : colunas) {
            List<Object> linhas = entityManager.createNativeQuery(
                            "SELECT p." + coluna + "::text FROM erp.people p WHERE p.id IN (:ids)")
                    .setParameter("ids", clienteIds)
                    .getResultList();
            linhas.stream().filter(java.util.Objects::nonNull).map(Object::toString).forEach(brutos::add);
        }
        return brutos;
    }

    @Transactional(readOnly = true, transactionManager = "erpTransactionManager")
    @SuppressWarnings("unchecked")
    public List<ClienteContrato> buscarPorVariantes(Collection<String> variantes) {
        StringBuilder where = new StringBuilder();
        for (String coluna : colunas) {
            if (where.length() > 0) {
                where.append(" OR ");
            }
            // RIGHT(..., 11) descarta +55/0 de cadastros antigos e deixa DDD + numero.
            where.append("RIGHT(regexp_replace(COALESCE(p.").append(coluna)
                    .append("::text, ''), '\\D', '', 'g'), 11) IN (:variantes)");
        }
        String sql = """
                SELECT DISTINCT p.id AS cliente_id, ac.contract_id AS contrato_id
                FROM erp.people p
                INNER JOIN erp.contracts c ON c.client_id = p.id
                INNER JOIN erp.authentication_contracts ac ON ac.contract_id = c.id
                WHERE %s
                ORDER BY p.id, ac.contract_id
                """.formatted(where);
        List<Object[]> linhas = entityManager.createNativeQuery(sql)
                .setParameter("variantes", variantes)
                .getResultList();
        List<ClienteContrato> resultado = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            resultado.add(new ClienteContrato(((Number) linha[0]).longValue(), ((Number) linha[1]).longValue()));
        }
        return resultado;
    }
}
