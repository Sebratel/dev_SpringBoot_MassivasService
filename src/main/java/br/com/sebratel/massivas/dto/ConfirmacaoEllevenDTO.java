package br.com.sebratel.massivas.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmacaoEllevenDTO {
    private boolean success;
    @Nullable
    private String message;
}
