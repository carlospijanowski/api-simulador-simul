package br.com.bancotoyota.services.simulador.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Simulação de Financiamentos")
                        .description("API de Simulação de Financiamentos")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banco Toyota Developers")
                                .url("https://developers.bancotoyota.com.br")
                                .email("api@bancotoyota.com.br")))
                 .servers(List.of(new Server().url("https://sandboxapi.bancotoyota.com.br/financiamentos/v1").description("Homolog"),
                        new Server().url("https://sandboxapidev.bancotoyota.com.br/financiamentos/v1").description("Develop")));
    }

}