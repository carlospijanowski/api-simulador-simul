package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.ComboWrapper;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.repository.SeguroPrestamistaPlusRepositoryImpl;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SeguroPrestamistaPlusServiceImplTest {
	
	@Mock
	private SeguroPrestamistaPlusRepositoryImpl repository;
	

	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();
	
	private SeguroPrestamistaPlusServiceImpl service;

	@Mock
	private TokenService tokenService;

	private AccessToken token;

	@Before
	public void setUp() {
		token = new AccessToken();
		token.setToken("Token");
		when(tokenService.generateDirectAccessToken())
				.thenReturn(token);
		this.service = new SeguroPrestamistaPlusServiceImpl(repository, new ConversorComboPrestamistaParaSeguro(), new FiltroPrestamistaPlus(), true, tokenService);
	}
	
	@Test
	public void deveRetornarSeguros() throws JsonMappingException, JsonProcessingException, IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		Combo combo = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class).getCombo();
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(combo);

		SeguroPrestamistaPlusResponse resultados = this.service.getSegurosPrestamista("", dataBA, null, false, false);
		
		assertTrue(!resultados.getSeguros().isEmpty());
		
		verify(this.repository, times(1)).findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString());
	}
	
	@Test
	public void naoDeveRetornarSegurosCasoDataComboMaiorQueDataBA() throws JsonMappingException, JsonProcessingException, IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		LocalDate dataCombo = LocalDate.of(2023, 6, 10);
		Combo combo = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class).getCombo();
		combo.setDataInicio(dataCombo);
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(combo);
		SeguroPrestamistaPlusResponse resultados = this.service.getSegurosPrestamista("", dataBA, null, false, false);
		
		assertTrue(resultados.getSeguros().isEmpty());
		
		verify(this.repository, times(1)).findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString());
	}
	
	@Test
	public void deveRetornarSeguroQuandoPassadoOTipo() throws JsonMappingException, JsonProcessingException, IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		Combo combo = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class).getCombo();
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(combo);
		Optional<Seguro> resultados = this.service.getSeguroPrestamista("", dataBA, TipoDeSeguroPrestamista.SPF);
		
		assertTrue(resultados.isPresent());
		
		verify(this.repository, times(1)).findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString());
	}
	
	@Test
	public void deveRetornarNuloQuandoNaoEncontrarOTipo() throws JsonMappingException, JsonProcessingException, IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		Combo combo = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class).getCombo();
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(combo);
		Optional<Seguro> resultados = this.service.getSeguroPrestamista("", dataBA, TipoDeSeguroPrestamista.NENHUM);
		
		assertFalse(resultados.isPresent());
		
		verify(this.repository, times(1)).findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString());
	}

	@Test
	public void deveRetornarListaSeguroVaziaQuandoFuncionalidadeParametrizadaComoDesligada() {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		SeguroPrestamistaPlusServiceImpl seguroPrestamistaPlusService = new SeguroPrestamistaPlusServiceImpl(repository, new ConversorComboPrestamistaParaSeguro(), new FiltroPrestamistaPlus(), false, tokenService);
		SeguroPrestamistaPlusResponse resultados = seguroPrestamistaPlusService.getSegurosPrestamista("", dataBA, null, false, false);
		assertTrue(resultados.getSeguros().isEmpty());
	}

	@Test
	public void deveSeguirProcessamentoComConsultaRetornarComboVazio() {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);

		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(new Combo());
		SeguroPrestamistaPlusResponse resultados = this.service.getSegurosPrestamista("", dataBA, null, false, false);

		assertTrue(resultados.getSeguros().isEmpty());
	}

	@Test
	public void quandoPrestamistaParametrizadoEntaoRetonarComboComSegurosDisponiveis() throws IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);

		Combo combo = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class).getCombo();
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(combo);
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista();
		parametrosDeSeguroPrestamista.setCnpjOrigemNegocio("123456789455");
		parametrosDeSeguroPrestamista.setSpfRemovido(false);
		parametrosDeSeguroPrestamista.setSvpRemovido(false);

		SeguroPrestamistaPlusResponse resultados = this.service.getSegurosPrestamistaByParametrizacao(dataBA, parametrosDeSeguroPrestamista);

		assertEquals(3, resultados.getSeguros().size());
	}

	@Test
	public void quandoPrestamistaNaoParametrizadoEntaoRetonarComboComSegurosDefault() throws IOException {
		LocalDate dataBA = LocalDate.of(2022, 6, 10);

		Combo comboDefault = fabrica.criarObjetoDoJson("oferta-prestamista/combo-default-response.json", ComboWrapper.class).getCombo();
		when(repository.findSegurosDisponiveisPorOrigemNegocio(anyString(), anyString()))
				.thenReturn(comboDefault);
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista();
		parametrosDeSeguroPrestamista.setCnpjOrigemNegocio("1231455665");

		SeguroPrestamistaPlusResponse resultados = this.service.getSegurosPrestamistaByParametrizacao(dataBA, parametrosDeSeguroPrestamista);

		assertEquals(2, resultados.getSeguros().size());
	}
}
