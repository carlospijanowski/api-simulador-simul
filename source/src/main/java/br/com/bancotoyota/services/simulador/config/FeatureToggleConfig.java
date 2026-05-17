package br.com.bancotoyota.services.simulador.config;

import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Getter
@Setter
public class FeatureToggleConfig {

    public FeatureToggleConfig() {

    }

    public FeatureToggleConfig(Boolean buscaApiMotorTaxaEnabled, boolean cicloIntermediariasEnabled) {
        this.buscaApiMotorTaxaEnabled = buscaApiMotorTaxaEnabled;
        this.cicloIntermediariasEnabled = cicloIntermediariasEnabled;
    }

    @Value("${busca-api-motor-taxa.enabled}")
    private Boolean buscaApiMotorTaxaEnabled;

    @Value("${ciclo-intermediarias.enabled}")
    private Boolean cicloIntermediariasEnabled;

}
