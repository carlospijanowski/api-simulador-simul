package br.com.bancotoyota.services.simulador.config;

import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
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
@ComponentScan("br.com.bancotoyota.services.simulador.repository")
@EnableRedisRepositories(basePackages = "br.com.bancotoyota.services.simulador.repository")
public class RedisConfig {

	@Value("${spring.redis.host}")
    private String redisHostName;

    @Value("${spring.redis.port}")	
    private int redisPort;
    
    @Value("${spring.redis.password}")	
    private String redisPassword;
	@Bean
	public JedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHostName, redisPort);
		config.setPassword(RedisPassword.of(redisPassword));
		JedisConnectionFactory connection = new JedisConnectionFactory(config);
		connection.getPoolConfig().setTestOnBorrow(true);
		return connection;
	}

	@Bean
    public RedisTemplate<String, TaxasIOF> redisIOFTemplate()  {
        final RedisTemplate<String, TaxasIOF> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(TaxasIOF.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, Seguro> seguroRedisTemplate()  {
        final RedisTemplate<String, Seguro> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Seguro.class));
        return template;
    }

    @Bean
    public RedisTemplate<String, ControlesBA> controlesBARedisTemplate()  {
        final RedisTemplate<String, ControlesBA> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ControlesBA.class));
        return template;
    }
}
