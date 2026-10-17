package br.com.sebratel.massivas.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Table(name = "authentication_sites")
@Data
@Entity
public class AuthenticationSiteEntity {
    @Id
    Long id;
    String title;
    String city;
    String neighborhood;
}
