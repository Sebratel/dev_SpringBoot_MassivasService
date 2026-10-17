package br.com.sebratel.massivas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contracts", schema = "erp")
public class ErpContract {
    @Id
    private Long id;
}