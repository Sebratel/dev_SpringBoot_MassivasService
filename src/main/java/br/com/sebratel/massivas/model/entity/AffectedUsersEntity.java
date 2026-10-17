package br.com.sebratel.massivas.model.entity;

import br.com.sebratel.massivas.enums.ClientType;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "assignment_clients")
@NoArgsConstructor
@AllArgsConstructor
public class AffectedUsersEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_pppoe")
    @Nullable
    String pppoe;
    @NotNull(message = "O ID do protocolo é obrigatório (protocol)")
    @Column(name = "protocol_id", nullable = false)
    Long protocol;
    @Column(name = "motive")
    String reason;
    @Enumerated(EnumType.ORDINAL)
    @Column(name="corporate_client")
    @NotNull(message = "O cliente deve ter um tipo")
    ClientType clientType;
    @NotNull(message = "O ID do contrato é obrigatório (contractId)")
    @Column(name = "contract_id", nullable = false)
    Long contractId;
    @NotNull(message = "A estimativa de finalização é obrigatória (finishDate)")
    @Column(name = "finish_date", nullable = false)
    LocalDateTime finishDate;
    LocalDateTime created;
    @Column(name = "created_by", updatable = false, nullable = false)
    @CreatedBy
    private String createdBy;
}
