package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.PerfilClienteDTO;
import br.com.sebratel.massivas.dto.PerfilClienteDTO.Assunto;
import br.com.sebratel.massivas.dto.PerfilClienteDTO.MassivasDoCliente;
import br.com.sebratel.massivas.repository.afetados.impl.MassivaHistoryJPARepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import br.com.sebratel.massivas.utils.TelefoneBrasil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Perfil do cliente para o resumo de recorrencia: cadastros e telefones do ERP e participacao em massivas
 * (massiva_history + massiva_affected_clients, tabelas permanentes). Falha do banco nunca derruba a consulta.
 */
@Service
@Slf4j
public class PerfilClienteService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_TELEFONES = 10;
    private static final int MAX_ASSUNTOS = 3;

    private final ClientePorTelefoneRepository clienteRepository;
    private final MassivaHistoryJPARepository historyRepository;

    public PerfilClienteService(ClientePorTelefoneRepository clienteRepository,
                                MassivaHistoryJPARepository historyRepository) {
        this.clienteRepository = clienteRepository;
        this.historyRepository = historyRepository;
    }

    /** Perfil a partir dos vinculos cliente/contrato ja localizados; vazio de cadastro se nao houver nenhum. */
    public PerfilClienteDTO perfil(Collection<ClienteContrato> vinculos) {
        var clientes = vinculos.stream().map(ClienteContrato::clienteId).distinct().toList();
        var contratos = vinculos.stream().map(ClienteContrato::contratoId).distinct().toList();
        var perfil = PerfilClienteDTO.builder().registrosPeople(clientes.size()).contratos(contratos.size());
        if (clientes.isEmpty()) {
            return perfil.telefones(List.of()).build();
        }
        perfil.telefones(telefones(clientes));
        perfil.massivas(massivas(contratos));
        return perfil.build();
    }

    private List<String> telefones(List<Long> clientes) {
        try {
            var canonicos = new TreeSet<String>();
            for (String bruto : clienteRepository.telefonesDosClientes(clientes)) {
                TelefoneBrasil.canonico(bruto).ifPresent(canonicos::add);
            }
            return canonicos.stream().limit(MAX_TELEFONES).toList();
        } catch (RuntimeException e) {
            log.warn("Telefones do cliente indisponiveis: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private MassivasDoCliente massivas(List<Long> contratos) {
        try {
            LocalDateTime agora = LocalDateTime.now(ZONA);
            LocalDateTime ano = agora.minusMonths(12);
            Timestamp ultima = historyRepository.ultimaAberturaDosContratos(contratos, ano);
            return MassivasDoCliente.builder()
                    .ultimos30Dias(historyRepository.contarDosContratos(contratos, agora.minusDays(30)))
                    .ultimos90Dias(historyRepository.contarDosContratos(contratos, agora.minusDays(90)))
                    .ultimos12Meses(historyRepository.contarDosContratos(contratos, ano))
                    .ultimaEm(ultima == null ? null : ultima.toLocalDateTime().toString())
                    .assuntos(historyRepository.titulosFrequentesDosContratos(contratos, ano).stream()
                            .limit(MAX_ASSUNTOS)
                            .map(t -> Assunto.builder().titulo(t.getTitulo()).vezes(t.getVezes()).build())
                            .toList())
                    .build();
        } catch (RuntimeException e) {
            log.warn("Massivas do cliente indisponiveis: {}", e.getClass().getSimpleName());
            return null;
        }
    }
}
