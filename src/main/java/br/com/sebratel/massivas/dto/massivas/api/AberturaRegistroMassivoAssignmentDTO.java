package br.com.sebratel.massivas.dto.massivas.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AberturaRegistroMassivoAssignmentDTO {
    private String title;
    private String description;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime finalDate;
    private Integer companyPlaceId;
}