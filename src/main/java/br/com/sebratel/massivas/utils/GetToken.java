package br.com.sebratel.massivas.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class GetToken {

    private static final String FILE_PATH = "network_logs.json";

    /**
     * Recupera o campo Authorization do arquivo network_logs.json
     * @return String contendo o Bearer token ou null se não encontrado
     */
    public static String retrieve() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File jsonFile = new File(FILE_PATH);

            // Verifica se o arquivo existe para evitar FileNotFoundException
            if (!jsonFile.exists()) {
                System.err.println("Erro: Arquivo " + FILE_PATH + " não encontrado.");
                return null;
            }

            // Lê o arquivo e navega até o campo "Authorization"
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode authNode = rootNode.get("Authorization");

            if (authNode != null && !authNode.isNull()) {
                return authNode.asText();
            }

        } catch (IOException e) {
            System.err.println("Erro ao ler o token do JSON: " + e.getMessage());
        }

        return null;
    }
}