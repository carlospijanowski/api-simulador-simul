package br.com.bancotoyota.services.simulador.config;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mongodb")
public class MultipleMongoProperties {

    private MongoProperties propostas = new MongoProperties();
    private MongoProperties simulacoesParceiros = new MongoProperties();
}
