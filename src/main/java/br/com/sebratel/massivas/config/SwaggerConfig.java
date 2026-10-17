package br.com.sebratel.massivas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BFF Sebratel API")
                        .version("1.0.0")
                        .description("BFF para integração de serviços ERP Elleven e Radius.")
                        .contact(new Contact()
                                .name("Time de Desenvolvimento")
                                .email("desenvolvimento@sebratel.com.br")));
    }
}