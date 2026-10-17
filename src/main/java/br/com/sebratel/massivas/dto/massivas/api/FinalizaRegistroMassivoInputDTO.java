package br.com.sebratel.massivas.dto.massivas.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FinalizaRegistroMassivoInputDTO{
    private String assignmentId;
    private String incidentStatusId;
    private String description;
    private String progress;
    private String priority;
    private String notificationTarget;
    private String privateReport;
}
