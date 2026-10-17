package br.com.sebratel.massivas.dto.massivas;

import java.time.LocalDateTime;

public record MassivaHistoryOutputDTO(
        Long id,
        String protocolo,
        String assignmentId,
        String status,
        String tituloIncidente,
        String pontoDeAcesso,
        Integer clientesAfetados,
        LocalDateTime abertoEm,
        LocalDateTime fechadoEm,
        LocalDateTime ultimaAcaoEm,
        String fechadoPor,
        String descricaoFechamento,
        String origem) {
}
