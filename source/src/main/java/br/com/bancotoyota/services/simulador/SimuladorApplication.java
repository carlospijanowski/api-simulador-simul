package br.com.bancotoyota.services.simulador;

import br.com.bancotoyota.services.simulador.config.AuthConfig;
import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableConfigurationProperties(AuthConfig.class)
@EnableRetry
public class SimuladorApplication {
	public static void main(String[] args) {
		GracefulshutdownSpringApplication.run(SimuladorApplication.class, args);
	}
}
