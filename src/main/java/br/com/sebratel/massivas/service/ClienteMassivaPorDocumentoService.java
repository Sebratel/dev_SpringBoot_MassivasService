package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.ClienteMassivaPorTelefoneOutputDTO;
import br.com.sebratel.massivas.repository.erp.ClientePorDocumentoRepository;
import br.com.sebratel.massivas.repository.erp.ClientePorTelefoneRepository.ClienteContrato;
import br.com.sebratel.massivas.utils.DocumentoBrasil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CPF/CNPJ informado pelo atendente -> contratos no ERP -> massiva de cada contrato. Mesma resposta da busca por
 * telefone, com o documento mascarado no lugar do telefone.
 */
@Service
@Slf4j
public class ClienteMassivaPorDocumentoService {

    private final ClientePorDocumentoRepository clientePorDocumentoRepository;
    private final ClienteMassivaPorTelefoneService massivaService;

    public ClienteMassivaPorDocumentoService(ClientePorDocumentoRepository clientePorDocumentoRepository,
                                             ClienteMassivaPorTelefoneService massivaService) {
        this.clientePorDocumentoRepository = clientePorDocumentoRepository;
        this.massivaService = massivaService;
    }

    public ClienteMassivaPorTelefoneOutputDTO consultar(String documento) {
        String digitos = DocumentoBrasil.normalizar(documento);
        String mascarado = DocumentoBrasil.mascarar(digitos);

        List<ClienteContrato> vinculos = clientePorDocumentoRepository.buscarPorDocumento(digitos);
        if (vinculos.isEmpty()) {
            log.info("Documento {} sem cliente com contrato no ERP", mascarado);
            return ClienteMassivaPorTelefoneOutputDTO.builder()
                    .status(ClienteMassivaPorTelefoneOutputDTO.CLIENTE_NAO_ENCONTRADO)
                    .documento(mascarado)
                    .build();
        }
        int clientes = (int) vinculos.stream().map(ClienteContrato::clienteId).distinct().count();
        log.info("Documento {}: {} cliente(s), {} contrato(s)", mascarado, clientes, vinculos.size());
        return massivaService.avaliarMassiva(vinculos, ClienteMassivaPorTelefoneOutputDTO.builder()
                .documento(mascarado)
                .clientesEncontrados(clientes));
    }
}
