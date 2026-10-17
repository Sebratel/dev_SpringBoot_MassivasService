package br.com.sebratel.massivas.dto.massivas;

import lombok.Data;

@Data
public class IncidentDTO {
    private Long id;
    private Integer teamId;
    private GenericTitleDTO team;
    private Integer incidentStatusId;
    private GenericTitleDTO incidentStatus;
    private Long clientId;
    private AssignmentDTO assignment;
    private AssignmentIncidentsDTO assignmentIncidents;
}
