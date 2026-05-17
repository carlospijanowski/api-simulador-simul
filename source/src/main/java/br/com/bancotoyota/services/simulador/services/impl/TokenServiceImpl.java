package br.com.bancotoyota.services.simulador.services.impl;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.config.AuthConfig;
import br.com.bancotoyota.services.simulador.services.TokenService;

import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
public class TokenServiceImpl implements TokenService{

    private AuthConfig authConfig;
    private RestTemplate restTemplate;
    private ObjectMapper mapper;
    private String informacoesAdicionaisParaLog;

    protected TokenServiceImpl(AuthConfig authConfig, RestTemplate restTemplate, ObjectMapper mapper) {
        this.authConfig = authConfig;
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public AccessToken generateAccessToken(){

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        /*
        log.info("Geração de Token");
        log.info("client_id : {}", authConfig.getClientIdParceiros());
        log.info("client_secret : {}", authConfig.getClientSecretParceiros());
        log.info("username : {}", authConfig.getUsernameParceiros());
        log.info("password : {}", authConfig.getPasswordParceiros());
        log.info("grant_type : {}", authConfig.getGrantType());
        */
        map.add("client_id", authConfig.getClientIdParceiros());
        map.add("client_secret", authConfig.getClientSecretParceiros());
        map.add("username", authConfig.getUsernameParceiros());
        map.add("password", authConfig.getPasswordParceiros());
        map.add("grant_type", authConfig.getGrantType());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<AccessToken> response = restTemplate.postForEntity(authConfig.getTokenUrl(), request , AccessToken.class);

        if(HttpStatus.OK != response.getStatusCode()){
            ThirdPartyException e = new ThirdPartyException("erro de geração do token de acesso - statusCode: " + response.getStatusCode().toString(), null);
            log.error(e.getMessage() + " : " + response.getBody());
            throw e;
        }

        return response.getBody();
    }

    public AccessToken generateDirectAccessToken(){

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.add("client_id", authConfig.getDirectClientId());
        map.add("client_secret", authConfig.getDirectClientSecret());
        map.add("username", authConfig.getDirectUsername());
        map.add("password", authConfig.getDirectPassword());
        map.add("grant_type", authConfig.getDirectGrantType());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<AccessToken> response = restTemplate.postForEntity(authConfig.getTokenUrl(), request , AccessToken.class);

        if (HttpStatus.OK != response.getStatusCode()) {
            ThirdPartyException e = new ThirdPartyException("erro de geração do token de acesso - statusCode: " + response.getStatusCode().toString(), null);
            log.error(e.getMessage() + " : " + response.getBody());
            throw e;
        }

        return response.getBody();
    }
	
}
