package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.ComboWrapper;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SeguroPrestamistaPlusRepositoryImplTest {
	@Mock
	private RestTemplate restTemplate;
	private SeguroPrestamistaPlusRepositoryImpl repository;

	@MockBean
	private CalculadoraSeguroMecanicaServices seguroFranquiaServices;

	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();

	@Before
	public void setUp() {
		this.repository = new SeguroPrestamistaPlusRepositoryImpl(restTemplate, "localhost");
	}

	@Test
	public void deveRetornarSegurosComSucesso() {
		ComboWrapper combo = this.getComboCompleto("prestamista-plus/combo-completo-response.json");

		when(restTemplate.exchange(anyString(), any(), any(),eq(ComboWrapper.class)))
				.thenReturn(ResponseEntity.ok(combo));
		
		Combo resposta =this.repository.findSegurosDisponiveisPorOrigemNegocio("", "") ;
		
		assertNotNull(resposta);

		verify(restTemplate,times(1)).exchange(anyString(), any(), any(),eq(ComboWrapper.class));
	
	}

	@Test
	public void deveRetornarSegurosComHttpClientErrorException() {
		when(restTemplate.exchange(anyString(), any(), any(),eq(ComboWrapper.class)))
				.thenThrow(HttpClientErrorException.class);

		Combo resposta = this.repository.findSegurosDisponiveisPorOrigemNegocio("", "") ;
		assertNotNull(resposta);
		assertTrue(CollectionUtils.isEmpty(resposta.getItens()));

		verify(restTemplate,times(1)).exchange(anyString(), any(), any(),eq(ComboWrapper.class));
	}

	@Test
	public void deveRetornarSegurosComHttpServerErrorException() {
		when(restTemplate.exchange(anyString(), any(), any(),eq(ComboWrapper.class)))
				.thenThrow(HttpServerErrorException.class);

		Combo resposta = this.repository.findSegurosDisponiveisPorOrigemNegocio("", "") ;
		assertNotNull(resposta);
		assertTrue(CollectionUtils.isEmpty(resposta.getItens()));

		verify(restTemplate,times(1)).exchange(anyString(), any(), any(),eq(ComboWrapper.class));
	}

	@Test
	public void deveRetornarSegurosComResourceAccessException() {
		when(restTemplate.exchange(anyString(), any(), any(),eq(ComboWrapper.class)))
				.thenThrow(ResourceAccessException.class);

		Combo resposta = this.repository.findSegurosDisponiveisPorOrigemNegocio("", "") ;
		assertNotNull(resposta);
		assertTrue(CollectionUtils.isEmpty(resposta.getItens()));

		verify(restTemplate,times(1)).exchange(anyString(), any(), any(),eq(ComboWrapper.class));
	}

	@Test
	public void deveRetornarSegurosComResponseStatusException() {
		when(restTemplate.exchange(anyString(), any(), any(),eq(ComboWrapper.class)))
				.thenThrow(ResponseStatusException.class);

		Combo resposta = this.repository.findSegurosDisponiveisPorOrigemNegocio("", "") ;
		assertNotNull(resposta);
		assertTrue(CollectionUtils.isEmpty(resposta.getItens()));

		verify(restTemplate,times(1)).exchange(anyString(), any(), any(),eq(ComboWrapper.class));
	}

	private ComboWrapper getComboCompleto(String path) {
		try {
			return fabrica.criarObjetoDoJson(path, ComboWrapper.class);
		} catch (IOException ex) {
			System.out.println("Erro inexperado");
		}

		return null;
	}

	
}
