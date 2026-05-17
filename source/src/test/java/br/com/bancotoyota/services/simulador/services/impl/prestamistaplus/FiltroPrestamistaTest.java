package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import java.io.IOException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.Assert.*;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.ComboWrapper;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;

@RunWith(SpringRunner.class)
public class FiltroPrestamistaTest {
	private FiltroPrestamistaPlus service= new FiltroPrestamistaPlus();
	private ConversorComboPrestamistaParaSeguro conversor= new ConversorComboPrestamistaParaSeguro();
	
	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();
	
	
	@Test
	public void deveFiltrarRegistrosDentroDataBA() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);
		
		assertTrue(!resultados.getSeguros().isEmpty());
		assertTrue(resultados.getSeguros().size() ==3);
		assertEquals(resultados.getSeguros().get(0).getTipo(),TipoDeSeguroPrestamista.SPF);
		assertEquals(resultados.getSeguros().get(1).getTipo(),TipoDeSeguroPrestamista.SVP);
		
		assertEquals(resultados.getSeguros().get(0).getOrdenacao(),1);
		assertEquals(resultados.getSeguros().get(1).getOrdenacao(),2);
		assertEquals(10008, resultados.getUltimoSeguro().getItemId());
	}
	
	@Test
	public void deveFiltrarRegistrosDentroDataBAAntesDaVigencia() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2020, 6, 10);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(resultados.getSeguros().isEmpty());

	}

	@Test
	public void deveFiltrarRegistrosDentroDataBAEDataVigenciaFimDefinida() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response-com-data-vigencia.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(!resultados.getSeguros().isEmpty());
		assertTrue(resultados.getSeguros().size() ==2);
		assertEquals(resultados.getSeguros().get(0).getTipo(),TipoDeSeguroPrestamista.SVP);

	}

	@Test
	public void deveFiltrarRegistrosDentroDataBAEDataVigenciaFimDefinidaVencendoNaDataBA() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response-com-data-vigencia-vencedo-data-ba.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 6);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(!resultados.getSeguros().isEmpty());
		assertTrue(resultados.getSeguros().size() ==2);
		assertEquals(resultados.getSeguros().get(0).getTipo(),TipoDeSeguroPrestamista.SPF);
		assertEquals(resultados.getSeguros().get(1).getTipo(),TipoDeSeguroPrestamista.SVP);

		assertEquals(resultados.getSeguros().get(0).getOrdenacao(),1);
		assertEquals(resultados.getSeguros().get(1).getOrdenacao(),3);

	}

	@Test
	public void deveFiltrarRegistrosQuandoFicarForaDataBA() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response-com-registro-fora-data-ba.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(!resultados.getSeguros().isEmpty());
		assertTrue(resultados.getSeguros().size() ==2);
		assertEquals(resultados.getSeguros().get(0).getTipo(),TipoDeSeguroPrestamista.SVP);
		assertEquals(resultados.getSeguros().get(1).getTipo(),TipoDeSeguroPrestamista.SVP);

		assertEquals(resultados.getSeguros().get(0).getOrdenacao(),2);
		assertEquals(resultados.getSeguros().get(1).getOrdenacao(),3);

	}

	@Test
	public void deveFiltrarRegistrosQuandoDataBAForNoFuturo() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response-com-registro-fora-data-ba.json", ComboWrapper.class);
		Combo combo =comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2023, 6, 10);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(!resultados.getSeguros().isEmpty());

	}


	@Test
	public void deveFiltrarRegistrosDentroDataBAPassandoUmItemExcluido() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		List<Integer> excluidos = new ArrayList<>();
		excluidos.add(10007);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, excluidos, false, false);

		assertTrue(!resultados.getSeguros().isEmpty());
		assertTrue(resultados.getSeguros().size() ==2);
		assertEquals(resultados.getSeguros().get(0).getTipo(),TipoDeSeguroPrestamista.SPF);
		assertEquals(resultados.getSeguros().get(1).getTipo(),TipoDeSeguroPrestamista.SVP);

		assertEquals(resultados.getSeguros().get(0).getOrdenacao(),1);
		assertEquals(resultados.getSeguros().get(1).getOrdenacao(),3);

	}

	@Test
	public void deveFiltrarRegistrosDentroDataBAPassandoTodosItemsExcluidos() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		List<Integer> excluidos = new ArrayList<>();
		excluidos.add(10006);
		excluidos.add(10007);
		excluidos.add(10008);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, excluidos, false, false);

		assertTrue(resultados.getSeguros().isEmpty());
	}

	@Test
	public void deveFiltrarRegistrosRemovendoTodosItemsSVP() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		List<Integer> excluidos = new ArrayList<>();
		excluidos.add(10007);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, excluidos, false, true);

		assertEquals(1, resultados.getSeguros().size());
		assertEquals(TipoDeSeguroPrestamista.SPF, resultados.getSeguros().get(0).getTipo());
	}


	@Test
	public void deveFiltrarRegistrosRemovendoTodosItemsSPF() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		Combo combo = comboWrapper.getCombo();
		List<Seguro> seguros = conversor.converter(combo);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		List<Integer> excluidos = new ArrayList<>();
		excluidos.add(10007);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, excluidos, true, false);

		assertEquals(1, resultados.getSeguros().size());
		assertEquals(TipoDeSeguroPrestamista.SVP, resultados.getSeguros().get(0).getTipo());
	}

	@Test
	public void deveFiltrarRegistrosQuandoDataBAIgualDataInicioEDataVigenciaFimVencida() throws JsonMappingException, JsonProcessingException, IOException {
		Seguro seguro = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(12), new BigDecimal(24), new BigDecimal(3), new BigDecimal(30), 0, 99);
		LocalDate dataBA = LocalDate.of(2022, 6, 10);
		seguro.setInicioVigencia(dataBA);
		seguro.setFinalVigencia(dataBA.minusDays(1));

		/*
		Este cenário não poderia existir
			Inicio vigência = DataBA
			Fim vigência < DataBA
		 */

		List<Seguro> seguros = Arrays.asList(seguro);
		SeguroPrestamistaPlusResponse resultados = service.executarFiltro(seguros, dataBA, null, false, false);

		assertTrue(resultados.getSeguros().isEmpty());
	}

}
