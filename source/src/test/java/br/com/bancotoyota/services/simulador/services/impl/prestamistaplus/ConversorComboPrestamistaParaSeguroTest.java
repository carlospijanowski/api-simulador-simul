package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.Assert.*;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.ComboWrapper;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Iten;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;

@RunWith(SpringRunner.class)
public class ConversorComboPrestamistaParaSeguroTest {
	private ConversorComboPrestamistaParaSeguro service= new ConversorComboPrestamistaParaSeguro();
	
	private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();
	
	@Test
	public void deveRetornarResultadoVazioQuandoNaoHouverCombo() {
		List<Seguro> resultado = service.converter(null);
		assertTrue(resultado.isEmpty());
	}
	
	@Test
	public void deveRetornarResultadoVazioQuandoNaoHouverItemsCombo() {
		List<Seguro> resultado = service.converter(new Combo());
		assertTrue(resultado.isEmpty());
	}
	
	@Test
	public void deveConverterComSucesso() throws JsonMappingException, JsonProcessingException, IOException {
		ComboWrapper comboWrapper = fabrica.criarObjetoDoJson("prestamista-plus/combo-completo-response.json", ComboWrapper.class);
		List<Seguro> seguros = service.converter(comboWrapper.getCombo());
		Combo combo = comboWrapper.getCombo();
		for(int i=0;i<seguros.size(); i++) {
			Seguro seguro = seguros.get(i);
			Iten item = combo.itens.get(i);
			
			assertEquals(seguro.getDescricao(),item.getDescricao());
			assertEquals(seguro.getFatorDesemprego(),item.getFatorDesemprego(),0);
			assertEquals(seguro.getFatorInvalidez(),item.getFatorInvalidez(),0);
			assertEquals(seguro.getFatorMorte(),item.getFatorMorte(),0);
			assertEquals(seguro.getFinalVigencia(),item.getFinalVigencia());
			assertEquals(seguro.getFormaCalculoDescricao(),item.getFormaCalculoDescricao());
			assertEquals(seguro.getFormaCalculoValor(),item.getFormaCalculoValor());
			assertEquals(seguro.getIdadeMaxima(),item.getIdadeMaxima());
			assertEquals(seguro.getIdadeMinima(),item.getIdadeMinima());
			assertEquals(seguro.getInicioVigencia(),item.getInicioVigencia());
			assertEquals(seguro.getItemId(),item.getItemId());
			assertEquals(seguro.getMaximoDaParcela(),BigDecimal.valueOf(item.getValorLimiteParcela()));
			assertEquals(seguro.getMaximoDoContrato(),BigDecimal.valueOf(item.getValorMaximoFinanciamentoContrato()));
			assertEquals(seguro.getMaximoPorCliente(),BigDecimal.valueOf(item.getValorMaximoPorCliente()));
			assertEquals(seguro.getOrdenacao(),item.getOrdenacao());
			assertEquals(seguro.getPercentualDoSeguro(),BigDecimal.valueOf(item.getFatorPrestamista()));
			assertEquals(seguro.getPercentualIofSpf(),item.getPercentualIofSpf(),0);
			assertEquals(seguro.getPercentualProLabore(),item.getPercentualProLabore(),0);
			assertEquals(seguro.getPrazoFinal(),item.getPrazoFinal(),0);
			assertEquals(seguro.getPrazoInicio(),item.getPrazoInicio(),0);
			assertEquals(seguro.getTempoMaximoContrato(),item.getTempoMaximoContrato(),0);
			assertEquals(seguro.getTipo(),TipoDeSeguroPrestamista.valueOf(item.getTipo()));
			
		}
		
		
	}

}
