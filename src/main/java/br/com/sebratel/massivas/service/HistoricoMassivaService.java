package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.HistoricoMassivaDTO;
import br.com.sebratel.massivas.repository.afetados.impl.HistoricoClienteProjection;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

/**
 * Massivas recentes (abertas ou encerradas nas ultimas horas) que atingiram contratos do cliente, a partir das
 * tabelas permanentes massiva_history e massiva_affected_clients. Falha do banco nunca derruba a consulta.
 */
@Service
@Slf4j
public class HistoricoMassivaService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_ITENS = 10;
    private static final int MAX_DESCRICAO = 300;

    private final MassivaHistoryJPARepository repository;
    private final int horas;

    public HistoricoMassivaService(MassivaHistoryJPARepository repository,
                                   @Value("${matrix.historico.horas:6}") int horas) {
        this.repository = repository;
        this.horas = horas;
    }

    public int horas() {
        return horas;
    }

    public List<HistoricoMassivaDTO> recentes(Collection<Long> contratos) {
        if (contratos.isEmpty()) {
            return List.of();
        }
        try {
            LocalDateTime desde = LocalDateTime.now(ZONA).minusHours(horas);
            List<HistoricoClienteProjection> linhas = repository.historicoDosContratos(contratos, desde);
            log.info("Historico de massiva: {} registro(s) nas ultimas {}h para {} contrato(s)", linhas.size(), horas,
                    contratos.size());
            return linhas.stream().limit(MAX_ITENS).map(HistoricoMassivaService::paraDto).toList();
        } catch (RuntimeException e) {
            log.warn("Historico de massiva indisponivel: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private static HistoricoMassivaDTO paraDto(HistoricoClienteProjection l) {
        return HistoricoMassivaDTO.builder()
                .protocolo(l.getProtocolo())
                .titulo(l.getTitulo())
                .status(l.getStatus())
                .abertaEm(texto(l.getAbertaEm()))
                .encerradaEm(texto(l.getEncerradaEm()))
                .descricaoEncerramento(cortar(l.getDescricao()))
                .origem(l.getOrigem())
                .contratosAfetados(l.getContratos())
                .build();
    }

    private static String texto(Timestamp t) {
        return t == null ? null : t.toLocalDateTime().toString();
    }

    private static String cortar(String s) {
        if (s == null || s.length() <= MAX_DESCRICAO) {
            return s;
        }
        return s.substring(0, MAX_DESCRICAO - 1) + "…";
    }
}
