package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO.ContratoMassiva;
import br.com.sebratel.massivas.dto.massivas.ImpactDetailsOutputDTO;
import br.com.sebratel.massivas.exceptions.ResourceNotFoundException;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import br.com.sebratel.massivas.utils.TelefoneBrasil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Telefone de quem ligou -> clientes/contratos no ERP -> massiva de cada contrato. Reaproveita o
 * {@link AffectedUserService}, a mesma fonte de GET /api/v1/afetados/contract/{id} e GET /api/v1/matrix.
 */
@Service
@Slf4j
public class ClienteMassivaPorTelefoneService {

    private final ClientePorTelefoneRepository clientePorTelefoneRepository;
    private final AffectedUserService affectedUserService;

    private final int limiteClientesPorTelefone;

    public ClienteMassivaPorTelefoneService(ClientePorTelefoneRepository clientePorTelefoneRepository,
                                            AffectedUserService affectedUserService,
                                            @Value("${matrix.telefone.max-clientes:5}") int limiteClientesPorTelefone) {
        this.clientePorTelefoneRepository = clientePorTelefoneRepository;
        this.affectedUserService = affectedUserService;
        this.limiteClientesPorTelefone = limiteClientesPorTelefone;
    }

    public ClienteMassivaPorTelefoneOutputDTO consultar(String telefone) {
        var variantes = TelefoneBrasil.variantes(telefone);
        String mascarado = TelefoneBrasil.mascarar(telefone);

        List<ClienteContrato> vinculos = clientePorTelefoneRepository.buscarPorVariantes(variantes);
        if (vinculos.isEmpty()) {
            log.info("Telefone {} sem cliente com contrato no ERP", mascarado);
            return ClienteMassivaPorTelefoneOutputDTO.builder()
                    .status(ClienteMassivaPorTelefoneOutputDTO.CLIENTE_NAO_ENCONTRADO)
                    .telefone(mascarado)
                    .build();
        }

        int clientes = (int) vinculos.stream().map(ClienteContrato::clienteId).distinct().count();
        if (clientes > limiteClientesPorTelefone) {
            // Na base o telefone da propria Sebratel esta em milhares de CPFs; alertar massiva por ele seria ruido.
            log.info("Telefone {} em {} clientes (limite {}): nao identifica quem ligou", mascarado, clientes,
                    limiteClientesPorTelefone);
            return ClienteMassivaPorTelefoneOutputDTO.builder()
                    .status(ClienteMassivaPorTelefoneOutputDTO.TELEFONE_COMPARTILHADO)
                    .telefone(mascarado)
                    .clientesEncontrados(clientes)
                    .build();
        }
        List<ContratoMassiva> emMassiva = vinculos.stream()
                .map(this::massivaDoContrato)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ContratoMassiva::getPrevisaoHoras))
                .toList();
        log.info("Telefone {}: {} cliente(s), {} contrato(s), {} em massiva",
                mascarado, clientes, vinculos.size(), emMassiva.size());

        var saida = ClienteMassivaPorTelefoneOutputDTO.builder()
                .telefone(mascarado)
                .clientesEncontrados(clientes)
                .contratosConsultados(vinculos.size());
        if (emMassiva.isEmpty()) {
            return saida.status(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA).build();
        }
        ContratoMassiva menor = emMassiva.get(0);
        return saida.status(ClienteMassivaPorTelefoneOutputDTO.EM_MASSIVA)
                .emMassiva(true)
                .previsaoHoras(menor.getPrevisaoHoras())
                .previsaoHora(menor.getPrevisaoHora())
                .motivo(menor.getMotivo())
                .contratosEmMassiva(emMassiva)
                .build();
    }

    private Optional<ContratoMassiva> massivaDoContrato(ClienteContrato vinculo) {
        try {
            return affectedUserService.getUsuariosAfetadosByContractId(vinculo.contratoId())
                    .getImpactedUsers().stream()
                    .map(entry -> entry.get(vinculo.contratoId()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(impacto -> paraContrato(vinculo, impacto));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    private ContratoMassiva paraContrato(ClienteContrato vinculo, ImpactDetailsOutputDTO impacto) {
        return ContratoMassiva.builder()
                .contratoId(vinculo.contratoId())
                .clienteId(vinculo.clienteId())
                .motivo(impacto.getReason())
                .previsaoHoras(impacto.getEstimateTimeOfRestoration())
                .previsaoHora(impacto.getEstimatedTimeHour())
                .build();
    }
}
