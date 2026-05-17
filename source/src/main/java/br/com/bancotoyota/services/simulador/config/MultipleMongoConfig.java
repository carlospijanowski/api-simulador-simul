package br.com.bancotoyota.services.simulador.config;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.*;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MultipleMongoProperties.class)
public class MultipleMongoConfig {

    private final MultipleMongoProperties mongoProperties;

    @Primary
    @Bean(name = "propostasMongoTemplate")
    public MongoTemplate propostasMongoTemplate() throws Exception {
        return new MongoTemplate(propostasFactory(this.mongoProperties.getPropostas()));
    }

    @Bean(name = "simulacoesParceirosMongoTemplate")
    public MongoTemplate simulacoesParceirosMongoTemplate() throws Exception {
        return new MongoTemplate(simulacoesParceirosFactory(this.mongoProperties.getSimulacoesParceiros()));
    }

    @Bean
    @Primary
    public MongoDatabaseFactory propostasFactory(final MongoProperties mongo) throws Exception {
        return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }

    @Bean
    public MongoDatabaseFactory simulacoesParceirosFactory(final MongoProperties mongo) throws Exception {
    	return new SimpleMongoClientDatabaseFactory(mongo.getUri());
    }

}