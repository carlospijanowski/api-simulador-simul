package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.ComboWrapper;
import br.com.bancotoyota.services.simulador.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Repository
@Slf4j
public class SeguroPrestamistaPlusRepositoryImpl {
	private RestTemplate restTemplate;
	private String prestamistaUrl;

	@Autowired
	public SeguroPrestamistaPlusRepositoryImpl(RestTemplate restTemplate,
			@Value("${seguro.prestamista.plus.url}") String prestamistaUrl) {
		this.restTemplate = restTemplate;
		this.prestamistaUrl = prestamistaUrl;
	}

	public Combo findSegurosDisponiveisPorOrigemNegocio(String token, String cnpjOrigemNegocio) {
		if (StringUtils.isEmpty(cnpjOrigemNegocio)) {
			cnpjOrigemNegocio = Constants.ZERO_STRING; //deve funcionar a chamada mesmo sem passar o cnpj
		}

		String enderecoFinalPrestamistaPlus = this.prestamistaUrl.concat("/" + cnpjOrigemNegocio);
		ResponseEntity<ComboWrapper> response;

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setBearerAuth(token);
		HttpEntity<Object> httpEntity = new HttpEntity<>(requestHeaders);

		try {
			response = restTemplate.exchange(enderecoFinalPrestamistaPlus, HttpMethod.GET, httpEntity, ComboWrapper.class);
		} catch (RestClientException| ResponseStatusException ex) {
			log.error("Erro ao chamar a api do prestamista plus: " + enderecoFinalPrestamistaPlus, ex);
			return new Combo();
		}
		return response.getBody().getCombo();
	}
}
