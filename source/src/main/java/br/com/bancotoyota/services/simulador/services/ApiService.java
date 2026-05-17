package br.com.bancotoyota.services.simulador.services;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class ApiService {

    /**
     * Obtem o cabecalho HTTP autorizado pelo token informado.
     * @param token token de autorizacao.
     * @return o cabecalho HTTP autorizado pelo token.
     */
    public HttpHeaders getAuthorizedHeader(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add("Authorization", "Bearer " + token);
        return httpHeaders;
    }

    public HttpHeaders getHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }
}
