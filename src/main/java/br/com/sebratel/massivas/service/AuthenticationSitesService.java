package br.com.sebratel.massivas.service;

import br.com.sebratel.massivas.dto.AuthenticationSitesOutputDTO;
import br.com.sebratel.massivas.repository.erp.AuthenticationSitesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationSitesService {

    AuthenticationSitesRepository authenticationSitesRepository;

    @Autowired
    public AuthenticationSitesService(AuthenticationSitesRepository authenticationSitesRepository) {
        this.authenticationSitesRepository = authenticationSitesRepository;
    }

    public List<AuthenticationSitesOutputDTO> execute(String title) {
        return authenticationSitesRepository.findByTitle(title).stream().map(AuthenticationSitesOutputDTO::fromEntity).toList();
    }

    /** Busca por título (contém, case-insensitive) para o seletor de site — usado no protocolo de backbone. */
    public List<AuthenticationSitesOutputDTO> search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }
        return authenticationSitesRepository
                .findTop50ByTitleContainingIgnoreCaseOrderByTitleAsc(q)
                .stream()
                .map(AuthenticationSitesOutputDTO::fromEntity)
                .toList();
    }
}
