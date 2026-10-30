package br.com.sebratel.massivas.repository.afetados.impl;

import java.sql.Timestamp;

/** Uma massiva (massiva_history) que atingiu contratos do cliente, com quantos deles ela atingiu. */
public interface HistoricoClienteProjection {
    Long getProtocolo();

    String getTitulo();

    String getStatus();

    Timestamp getAbertaEm();

    Timestamp getEncerradaEm();

    String getDescricao();

    String getOrigem();

    Long getContratos();
}
