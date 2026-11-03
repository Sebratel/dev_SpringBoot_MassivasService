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

import java.time.LocalDateTime;
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

    private final ClientePorTelefoneRepository clienteRepository;
    private final AffectedUserService affectedUserService;

    private final int limiteClientesPorTelefone;
    private final AutoIspService autoIspService;
    private final HistoricoMassivaService historicoService;
    private final PerfilClienteService perfilService;
    private final SituacaoErpService situacaoErp;

    public ClienteMassivaPorTelefoneService(ClientePorTelefoneRepository clienteRepository,
                                            AffectedUserService affectedUserService,
                                            @Value("${matrix.telefone.max-clientes:5}") int limiteClientesPorTelefone,
                                            AutoIspService autoIspService,
                                            HistoricoMassivaService historicoService,
                                            PerfilClienteService perfilService,
                                            SituacaoErpService situacaoErp) {
        this.clienteRepository = clienteRepository;
        this.affectedUserService = affectedUserService;
        this.limiteClientesPorTelefone = limiteClientesPorTelefone;
        this.autoIspService = autoIspService;
        this.historicoService = historicoService;
        this.perfilService = perfilService;
        this.situacaoErp = situacaoErp;
    }

    public ClienteMassivaPorTelefoneOutputDTO consultar(String telefone) {
        var variantes = TelefoneBrasil.variantes(telefone);
        String mascarado = TelefoneBrasil.mascarar(telefone);

        List<ClienteContrato> vinculos = clienteRepository.buscarPorVariantes(variantes);
        if (vinculos.isEmpty()) {
            // Sem contrato com conexao (ex.: cliente so com contratos cancelados): o ERP ainda tem o que dizer.
            var clientes = clienteRepository.clientesPorVariantes(variantes);
            if (clientes.size() == 1) { // com mais de um cliente o telefone nao diz de quem e o contrato
                log.info("Telefone {} so em cliente(s) sem contrato com conexao ({})", mascarado, clientes.size());
                return semConexao(clientes, ClienteMassivaPorTelefoneOutputDTO.builder().telefone(mascarado));
            }
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
        // Telefone de mais de um cliente nao diz de quem e o contrato bloqueado: sem bloco do ERP (a confirmacao
        // do CPF/CNPJ pelo atendente resolve, como no historico).
        return avaliarMassiva(vinculos, ClienteMassivaPorTelefoneOutputDTO.builder()
                .telefone(mascarado)
                .clientesEncontrados(clientes), clientes == 1);
    }

    /**
     * Massiva de cada contrato dos vinculos. Compartilhado com a busca por documento: {@code base} ja traz o que
     * identifica a consulta (telefone ou documento mascarado e quantidade de clientes).
     */
    ClienteMassivaPorTelefoneOutputDTO avaliarMassiva(List<ClienteContrato> vinculos,
            ClienteMassivaPorTelefoneOutputDTO.ClienteMassivaPorTelefoneOutputDTOBuilder base, boolean mostrarErp) {
        List<ContratoMassiva> emMassiva = vinculos.stream()
                .map(this::massivaDoContrato)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(ContratoMassiva::getPrevisaoHoras))
                .toList();
        log.info("{} contrato(s) consultados, {} em massiva", vinculos.size(), emMassiva.size());

        var contratos = vinculos.stream().map(ClienteContrato::contratoId).distinct().toList();
        var clienteIds = vinculos.stream().map(ClienteContrato::clienteId).distinct().toList();
        var autoisp = autoIspService.situacao(contratos);
        boolean emAutoisp = autoisp != null && autoisp.getEventos() != null && !autoisp.getEventos().isEmpty();
        var saida = base.contratosConsultados(vinculos.size()).autoisp(autoisp).emAutoisp(emAutoisp)
                .historico(historicoService.recentes(contratos)).historicoHoras(historicoService.horas())
                .perfil(perfilService.perfil(vinculos))
                .contratosSituacao(mostrarErp ? situacaoErp.situacaoContratos(clienteIds) : null)
                .protocolosErp(mostrarErp ? situacaoErp.protocolosAbertos(clienteIds) : null);
        if (emMassiva.isEmpty()) {
            return saida.status(ClienteMassivaPorTelefoneOutputDTO.SEM_MASSIVA).build();
        }
        ContratoMassiva menor = emMassiva.get(0);
        return saida.status(ClienteMassivaPorTelefoneOutputDTO.EM_MASSIVA)
                .emMassiva(true)
                .previsaoHoras(menor.getPrevisaoHoras())
                .previsaoHora(menor.getPrevisaoHora())
                .previsaoEm(menor.getPrevisaoEm())
                .previsaoVencida(menor.getPrevisaoVencida())
                .motivo(menor.getMotivo())
                .contratosEmMassiva(emMassiva)
                .build();
    }

    /**
     * Cliente que existe no ERP mas nao tem contrato com conexao: nao ha massiva a avaliar, mas o atendente ainda
     * precisa saber se o contrato esta cancelado e se ha protocolo aberto. Compartilhado com a busca por documento.
     */
    ClienteMassivaPorTelefoneOutputDTO semConexao(List<Long> clienteIds,
            ClienteMassivaPorTelefoneOutputDTO.ClienteMassivaPorTelefoneOutputDTOBuilder base) {
        return base.status(ClienteMassivaPorTelefoneOutputDTO.CLIENTE_SEM_CONEXAO)
                .clientesEncontrados(clienteIds.size())
                .contratosSituacao(situacaoErp.situacaoContratos(clienteIds))
                .protocolosErp(situacaoErp.protocolosAbertos(clienteIds))
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
                .previsaoEm(impacto.getEstimatedFinish() == null ? null : impacto.getEstimatedFinish().toString())
                .previsaoVencida(impacto.getEstimatedFinish() == null ? null
                        : !impacto.getEstimatedFinish().isAfter(LocalDateTime.now()))
                .build();
    }
}
