package br.com.sebratel.massivas.dto.massivas;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentDTO {
    private Long id;
    private String title;
    private String assignmentType;
    private int progress;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime finalDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime beginningDate;

    private Long responsibleId;
    private Object responsible;
    private Long requestorId;
    private RequestorDTO requestor;
    private Object inExecution;
}
