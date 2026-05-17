package br.com.bancotoyota.services.simulador.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="security.oauth2", ignoreUnknownFields=false)
@Getter
@Setter
public class AuthConfig {

    private String tokenUrl;
    private String clientIdParceiros;
    private String clientSecretParceiros;
    private String usernameParceiros;
    private String passwordParceiros;
    private String grantType;
    private String serviceUrl;	
	
    private String trustStorePassword;
    private String trustStore;

    private String directUsername;
    private String directPassword;
    private String directGrantType;
    private String directClientId;
    private String directClientSecret;

}
