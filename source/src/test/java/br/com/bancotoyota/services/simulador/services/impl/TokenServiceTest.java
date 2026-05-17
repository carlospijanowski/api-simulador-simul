package br.com.bancotoyota.services.simulador.services.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.config.AuthConfig;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(AuthConfig.class)
public class TokenServiceTest {
	
	private TokenService tokenService;
	
	@Mock
	private RestTemplate restTemplate;
	
	@Before
	public void init() {
		AuthConfig config = new AuthConfig();
		config.setClientIdParceiros("");
		config.setClientSecretParceiros("");
		config.setGrantType("");
		config.setPasswordParceiros("");
		config.setServiceUrl("");
		config.setTokenUrl("");
		config.setTrustStore("");
		config.setUsernameParceiros("");
		config.setDirectClientId("");
		config.setDirectClientSecret("");
		config.setDirectGrantType("");
		config.setDirectPassword("");
		config.setDirectUsername("");

		ObjectMapper mapper = new ObjectMapper();
		
		tokenService = new TokenServiceImpl(config, restTemplate, mapper);
	}
	
	@Test
	public void deveGerarToken() {
		AccessToken tokenMockado = new AccessToken();
		tokenMockado.setToken("1234");
		ResponseEntity<AccessToken> responseEntity = new ResponseEntity<>(tokenMockado, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(), eq(AccessToken.class) ))
				.thenReturn(responseEntity);
		
		AccessToken token = tokenService.generateAccessToken();
		
		assertEquals(tokenMockado.getToken(), token.getToken());
	}

	@Test(expected= ThirdPartyException.class)
	public void deveOcorrerErroAoGerarToken() {
		AccessToken tokenMockado = new AccessToken();
		tokenMockado.setToken("1234");
		ResponseEntity<AccessToken> responseEntity = new ResponseEntity<>(tokenMockado, HttpStatus.BAD_REQUEST);
		when(restTemplate.postForEntity(anyString(), any(), eq(AccessToken.class) )).thenReturn(responseEntity);

		tokenService.generateAccessToken();

	}

	@Test
	public void deveGerarTokenParaODirect() {
		AccessToken tokenMockado = new AccessToken();
		tokenMockado.setToken("1234");
		ResponseEntity<AccessToken> responseEntity = new ResponseEntity<>(tokenMockado, HttpStatus.OK);
		when(restTemplate.postForEntity(anyString(), any(), eq(AccessToken.class) ))
				.thenReturn(responseEntity);

		AccessToken token = tokenService.generateDirectAccessToken();

		assertEquals(tokenMockado.getToken(), token.getToken());
	}

	@Test(expected= ThirdPartyException.class)
	public void deveOcorrerErroAoGerarTokenParaODirect() {
		AccessToken tokenMockado = new AccessToken();
		tokenMockado.setToken("1234");
		ResponseEntity<AccessToken> responseEntity = new ResponseEntity<>(tokenMockado, HttpStatus.BAD_REQUEST);
		when(restTemplate.postForEntity(anyString(), any(), eq(AccessToken.class) ))
				.thenReturn(responseEntity);

		tokenService.generateDirectAccessToken();

	}
}

