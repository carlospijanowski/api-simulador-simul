package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.Estado;
import br.com.bancotoyota.services.simulador.entities.EstadoResponse;
import br.com.bancotoyota.services.simulador.entities.UfEmplacamento;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.EmplacamentoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class EmplacamentoServiceImpl implements EmplacamentoService {

    private RestTemplate restTemplate;
    private ApiService apiService;
    private String apiEmplacamantosUrl;

    @Autowired
    public EmplacamentoServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                   @Value("${api.emplacamentos.url}") String apiEmplacamantosUrl) {
        this.restTemplate = restTemplate;
        this.apiEmplacamantosUrl = apiEmplacamantosUrl;
        this.apiService = apiService;
    }

    @Override
    public Estado getEstado(UfEmplacamento uf, String token) {
        log.info("Obtendo o estado com a UF {}...", uf);
        try {
            String url = apiEmplacamantosUrl.concat("/estados");
            log.info("Endpoint: {}", url);
            ResponseEntity<EstadoResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(token)), EstadoResponse.class);

            List<Estado> estados = responseEntity.getBody().getResponse().getEstados();
            return estados.stream().filter(estado -> estado.getUf().equals(uf.name()))
                    .findFirst().orElseThrow(() -> new EntityNotFoundException(Estado.class, "uf-emplacamento"));
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException("Erro na API de emplacamentos", e);
        }
    }
}
