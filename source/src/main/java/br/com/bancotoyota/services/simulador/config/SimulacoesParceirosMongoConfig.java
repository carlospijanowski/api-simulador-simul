package br.com.bancotoyota.services.simulador.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "br/com/bancotoyota/services/simulador/repository/simulacoes",
        mongoTemplateRef = "simulacoesParceirosMongoTemplate")
public class SimulacoesParceirosMongoConfig {

}
