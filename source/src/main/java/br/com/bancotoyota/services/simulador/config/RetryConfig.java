package br.com.bancotoyota.services.simulador.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class RetryConfig {
	private  int numeroRetentativas;
	private  int delayRetentativas;
	
	@Autowired
	public RetryConfig(@Value("${spring.retry.numero-retentativas}") int numeroRententativasConfigurada, @Value("${spring.retry.delay-entre-retentativas-ms}")int delayRetentativasConfigurada) {
		this.numeroRetentativas = numeroRententativasConfigurada;
		this.delayRetentativas = delayRetentativasConfigurada;
	}
}
