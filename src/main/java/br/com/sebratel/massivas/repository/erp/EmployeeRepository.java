package br.com.sebratel.massivas.repository.erp;

import br.com.sebratel.massivas.model.entity.PersonEntity;
import br.com.sebratel.massivas.repository.erp.projections.InsigniaProjection;
import br.com.sebratel.massivas.repository.erp.projections.PersonContactProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("erpEmployeeRepository")
public interface EmployeeRepository extends JpaRepository<PersonEntity, Long> {

    /**
     * IDs de pessoa para um e-mail. Retorna LISTA (não Optional) de propósito: o e-mail pode
     * casar com mais de uma linha — seja porque {@code v_users} tem duplicata do e-mail, seja
     * porque há mais de um registro em {@code people} com o mesmo e-mail. Com {@code Optional},
     * o Hibernate lançava {@code NonUniqueResultException} (HTTP 500) nesse caso.
     *
     * <p>Ordenação determinística com dois critérios: primeiro o cadastro <b>completo</b> — o que
     * tem {@code tx_id} (CPF/CNPJ) preenchido — vem antes do duplicado "vazio"; como desempate,
     * o menor {@code id} (registro mais antigo). Assim o service pega sempre o cadastro real da
     * pessoa, não um fantasma sem documento. Ex. real: e-mail casou com id 89822 (com CPF) e
     * 302328 (sem CPF) → escolhe 89822. A deduplicação (o join pode repetir o mesmo id quando
     * {@code v_users} tem a linha duplicada) e o WARN de ambiguidade ficam no service — não dá
     * para usar {@code SELECT DISTINCT} aqui porque o Postgres exige que a expressão do
     * {@code ORDER BY} esteja no SELECT.</p>
     */
    @Query(value = """
            SELECT
                p.id
            FROM
                v_users vu
            INNER JOIN
                people p on vu.email = p.email
            WHERE
                vu.email = :email
            ORDER BY
                (NULLIF(TRIM(p.tx_id), '') IS NULL),
                p.id
            """, nativeQuery = true)
    List<Long> findPersonIdsByEmail(String email);

    @Query(value = """
            SELECT
                p.name AS name,
                p.email AS email
            FROM
                people p
            WHERE
                p.id = :personId
            """, nativeQuery = true)
    Optional<PersonContactProjection> findContactByPersonId(@Param("personId") Long personId);

    @Query(value = """
        select EXISTS(
            SELECT 
                    1
            FROM 
                    authentication_contracts ac
            JOIN 
                    contracts c ON c.id = ac.contract_id
            JOIN 
                    people p ON p.id = c.client_id
            JOIN 
                    insignias i ON i.id = p.insignia_id
            where  
                    c.id IN (:list)
            AND
                    i.title IN ('Contrato Corporativo PME', 'Contrato Corporativo')
        )
    """, nativeQuery = true)
    boolean hasB2BinInput(@Param("list") List<Long> list);

    @Query(value = """
        SELECT DISTINCT
                i.id as id,
                i.code as code,
                i.title as title
        FROM
                people p
        JOIN
                insignias i ON i.id = p.insignia_id
        WHERE
                p.tx_id = :txId
        ORDER BY
                i.id
    """, nativeQuery = true)
    List<InsigniaProjection> findInsigniasByTxId(@Param("txId") String txId);

    @Query(value = """
        SELECT
                p.name
        FROM
                people p
        WHERE
                p.tx_id = :txId
    """, nativeQuery = true)
    List<String> findByTxId(@Valid @NotNull String txId);

    @Query(value = """
        SELECT
                p.tx_id
        FROM
                people p
        WHERE
                p.email = :email
    """, nativeQuery = true)
    String findTxIdByEmail(@Valid @NotNull String email);

    @Query(value = """
        select p.tx_id 
            from authentication_contracts ac
                                inner join contracts c on c.id = ac.contract_id
                                inner join people p on p.id = c.client_id
                                where ac.contract_id = :contract;
    """, nativeQuery = true)
    String findTxIdByContract(@Valid @NotNull Long contract); // Alterado de String para Long
}
