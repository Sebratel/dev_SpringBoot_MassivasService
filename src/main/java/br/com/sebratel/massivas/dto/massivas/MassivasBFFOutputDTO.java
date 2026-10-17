package br.com.sebratel.massivas.dto.massivas;

import java.time.LocalDateTime;

public record MassivasBFFOutputDTO(
        Long id,
        LocalDateTime criacao,
        LocalDateTime finalizado,
        LocalDateTime sla,
        String protocolo,
        String equipe,
        String status,
        String tituloIncidente,
        String criadoPor,
        String responsavel,
        String catalogo,
        String pontoDeAcesso,
        String descricao) {
}
