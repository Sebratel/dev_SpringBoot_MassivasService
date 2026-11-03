package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.ContratosSituacaoDTO;
import br.com.sebratel.massivas.dto.ContratosSituacaoDTO.Problema;
import br.com.sebratel.massivas.dto.ProtocolosErpDTO;
import br.com.sebratel.massivas.repository.erp.ProtocoloErpRepository;
import br.com.sebratel.massivas.repository.erp.SituacaoContratoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * O que o ERP diz do cliente e o atendente do suporte precisa na hora da ligacao: contrato bloqueado, suspenso ou
 * cancelado e protocolos/visitas ja abertos. Falha do ERP nunca derruba a consulta de massiva: o bloco sai com
 * {@code disponivel=false}.
 */
@Service
@Slf4j
public class SituacaoErpService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    static final int MAX_PROBLEMAS = 3;
    static final int MAX_PROTOCOLOS = 5;
    private static final Set<String> SITUACOES_DE_VISITA = Set.of("agendado", "visita 1", "visita 2");
    private static final List<String> MARCAS_TECNICAS = List.of("MANUT", "SUPORTE", "CGR", "NOC", "VISITA", "INFRA");

    private final SituacaoContratoRepository contratos;
    private final ProtocoloErpRepository protocolos;
    private final int canceladosMeses;
    private final int protocoloAntigoDias;

    public SituacaoErpService(SituacaoContratoRepository contratos, ProtocoloErpRepository protocolos,
                              @Value("${matrix.contrato.cancelados-meses:12}") int canceladosMeses,
                              @Value("${matrix.protocolo.antigo-dias:180}") int protocoloAntigoDias) {
        this.contratos = contratos;
        this.protocolos = protocolos;
        this.canceladosMeses = canceladosMeses;
        this.protocoloAntigoDias = protocoloAntigoDias;
    }

    /** Situacao dos contratos dos clientes (people.id), com o ERP indisponivel tratado como bloco ausente. */
    public ContratosSituacaoDTO situacaoContratos(Collection<Long> clienteIds) {
        if (clienteIds.isEmpty()) {
            return null;
        }
        try {
            var linhas = contratos.problemasDosClientes(clienteIds);
            return montarContratos(contratos.contarContratos(clienteIds), linhas, LocalDateTime.now(ZONA));
        } catch (RuntimeException e) {
            log.warn("Situacao dos contratos indisponivel: {}", e.getClass().getSimpleName());
            return ContratosSituacaoDTO.builder().disponivel(false).build();
        }
    }

    /** Protocolos em aberto dos clientes (people.id) no ERP. */
    public ProtocolosErpDTO protocolosAbertos(Collection<Long> clienteIds) {
        if (clienteIds.isEmpty()) {
            return null;
        }
        try {
            var linhas = protocolos.abertosDosClientes(clienteIds);
            var recentes = linhas.stream().limit(MAX_PROTOCOLOS).toList();
            var visitas = protocolos.proximasVisitas(recentes.stream()
                    .map(ProtocoloErpRepository.Linha::assignmentId).toList());
            return montarProtocolos(linhas, visitas, LocalDateTime.now(ZONA));
        } catch (RuntimeException e) {
            log.warn("Protocolos do ERP indisponiveis: {}", e.getClass().getSimpleName());
            return ProtocolosErpDTO.builder().disponivel(false).build();
        }
    }

    ContratosSituacaoDTO montarContratos(int total, List<SituacaoContratoRepository.Linha> linhas,
                                         LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        LocalDate corteCancelados = hoje.minusMonths(canceladosMeses);
        List<Problema> problemas = linhas.stream()
                .map(l -> paraProblema(l, hoje))
                .filter(Objects::nonNull)
                // cancelamento antigo e ruido (o cliente ja voltou ou nao e mais cliente): so os recentes
                .filter(p -> !ContratosSituacaoDTO.CANCELADO.equals(p.getSituacao())
                        || !LocalDate.parse(p.getDesde()).isBefore(corteCancelados))
                .toList();
        // bloqueios primeiro (mais antigos antes); entre cancelados, o mais recente primeiro
        problemas = ordenar(problemas);
        int outros = Math.max(0, problemas.size() - MAX_PROBLEMAS);
        return ContratosSituacaoDTO.builder().disponivel(true).totalContratos(total)
                .problemas(problemas.stream().limit(MAX_PROBLEMAS).toList()).outros(outros).build();
    }

    private static List<Problema> ordenar(List<Problema> problemas) {
        var ativos = problemas.stream().filter(p -> !ContratosSituacaoDTO.CANCELADO.equals(p.getSituacao()))
                .sorted(Comparator.comparing(Problema::getDias, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        var cancelados = problemas.stream().filter(p -> ContratosSituacaoDTO.CANCELADO.equals(p.getSituacao()))
                .sorted(Comparator.comparing(Problema::getDias, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return java.util.stream.Stream.concat(ativos.stream(), cancelados.stream()).toList();
    }

    private static Problema paraProblema(SituacaoContratoRepository.Linha l, LocalDate hoje) {
        String situacao = switch (l.status()) {
            case "Bloqueio Financeiro" -> ContratosSituacaoDTO.BLOQUEIO_FINANCEIRO;
            case "Bloqueio Administrativo" -> ContratosSituacaoDTO.BLOQUEIO_ADMINISTRATIVO;
            case "Suspenso" -> ContratosSituacaoDTO.SUSPENSO;
            case "Cancelado" -> ContratosSituacaoDTO.CANCELADO;
            default -> null;
        };
        if (situacao == null) {
            return null;
        }
        boolean cancelado = ContratosSituacaoDTO.CANCELADO.equals(situacao);
        LocalDate desde = cancelado
                ? (l.cancelamento() != null ? l.cancelamento() : l.inicio() == null ? null : l.inicio().toLocalDate())
                : (l.inicio() == null ? null : l.inicio().toLocalDate());
        if (desde == null) {
            // sem data nao ha como calcular dias nem aplicar o corte: cancelado some, bloqueio fica sem tempo
            if (cancelado) {
                return null;
            }
            return Problema.builder().contratoId(l.contratoId()).situacao(situacao).build();
        }
        return Problema.builder().contratoId(l.contratoId()).situacao(situacao).desde(desde.toString())
                .dias(Math.max(0, ChronoUnit.DAYS.between(desde, hoje)))
                .motivo(cancelado ? motivoCancelamento(l.motivo()) : null).build();
    }

    /** "Cancelamento - Cliente Não Instalado - Desistência" vira "Cliente Não Instalado - Desistência". */
    static String motivoCancelamento(String evento) {
        if (evento == null || evento.isBlank()) {
            return null;
        }
        return evento.replaceFirst("^Cancelamento\\s*-\\s*", "").trim();
    }

    ProtocolosErpDTO montarProtocolos(List<ProtocoloErpRepository.Linha> linhas,
                                      Map<Long, LocalDateTime> visitas, LocalDateTime agora) {
        var itens = linhas.stream().limit(MAX_PROTOCOLOS).map(l -> {
            long dias = l.criado() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(l.criado(), agora));
            LocalDateTime visita = visitas.get(l.assignmentId());
            return ProtocolosErpDTO.Item.builder().protocolo(l.protocolo()).tipo(l.tipo()).situacao(l.situacao())
                    .abertoEm(l.criado() == null ? null : l.criado().toString()).haDias(dias)
                    .tecnico(tecnico(l.tipo(), l.situacao())).antigo(dias > protocoloAntigoDias)
                    .visitaEm(visita == null ? null : visita.toString()).build();
        }).toList();
        return ProtocolosErpDTO.builder().disponivel(true).total(linhas.size()).itens(itens).build();
    }

    /** Suporte tecnico, manutencao, CGR/NOC, infra ou visita: o que o atendente nao deve abrir de novo. */
    static boolean tecnico(String tipo, String situacao) {
        String t = tipo == null ? "" : tipo.toUpperCase(Locale.ROOT);
        if (t.startsWith("TEC") || MARCAS_TECNICAS.stream().anyMatch(t::contains)) {
            return true;
        }
        return situacao != null && SITUACOES_DE_VISITA.contains(situacao.trim().toLowerCase(Locale.ROOT));
    }
}
