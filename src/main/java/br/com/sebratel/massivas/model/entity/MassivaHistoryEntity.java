package br.com.sebratel.massivas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "massiva_history")
@NoArgsConstructor
@AllArgsConstructor
public class MassivaHistoryEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "protocol")
    Long protocol;
    @Column(name = "assignment_id")
    Long assignmentId;
    @Column(name = "access_point_code")
    String accessPointCode;
    @Column(name = "title")
    String title;
    @Column(name = "affected_clients")
    Integer affectedClients;
    @Column(name = "status")
    String status;
    @Column(name = "opened_at")
    LocalDateTime openedAt;
    @Column(name = "closed_at")
    LocalDateTime closedAt;
    @Column(name = "close_description")
    String closeDescription;
    @Column(name = "closed_by")
    String closedBy;
    @Column(name = "source")
    String source;
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
