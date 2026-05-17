package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.MessageHolder;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocioResponse;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.OrigemNegocioService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class OrigemNegocioServiceImpl implements OrigemNegocioService {

    private RestTemplate restTemplate;
    private ApiService apiService;
    private String apiOrigensNegociosUrl;

    @Autowired
    public OrigemNegocioServiceImpl(RestTemplate restTemplate, ApiService apiService,
                                    @Value("${api.origens-negocios.url}") String apiOrigensNegociosUrl) {
        this.restTemplate = restTemplate;
        this.apiOrigensNegociosUrl = apiOrigensNegociosUrl;
        this.apiService = apiService;
    }

    @Override
    public OrigemNegocio getOrigemNegocio(String cnpj, String token) {
        log.info("Obtendo a origem negocio com o CNPJ {}...", cnpj);
        try {
            String url = apiOrigensNegociosUrl.concat("/").concat(cnpj);
            log.info("Endpoint: {}", url);

            ResponseEntity<OrigemNegocioResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(apiService.getAuthorizedHeader(token)), OrigemNegocioResponse.class);
            return responseEntity.getBody().getResponse().getOrigemNegocio();
        } catch (HttpClientErrorException.NotFound e) {
            if (JsonUtils.canConvert(e.getResponseBodyAsString(), new TypeReference<List<MessageHolder>>(){})) {
                log.error(String.format("Não foi encontrada uma origem negóco com CNPJ '%s'", cnpj));
                throw new EntityNotFoundException(OrigemNegocio.class, "cnpj-origem-negocio");
            }
            throw new ThirdPartyException("Erro na API de origens negócios", e);
        } catch (HttpStatusCodeException e) {
            throw new ThirdPartyException("Erro na API de origens negócios", e);
        }
    }
}
