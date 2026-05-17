package br.com.bancotoyota.services.simulador.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroFranquiaServicesImpl;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroMecanicaServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaBalaoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoTaxaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SubsidioRequest;

import br.com.bancotoyota.services.simulador.services.CalculadoraServices;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;

import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
public class SimuladorTaxasControllerTest {
	
	private SimuladorTaxasController simuladorTaxasController;
	
	private CalculadoraServices calculadoraService = new CalculadoraServicesImpl();

	private CalculadoraSeguroFranquiaServices seguroFranquiaServices = new CalculadoraSeguroFranquiaServicesImpl();


	private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices = new CalculadoraSeguroMecanicaServicesImpl();


	@MockBean
    private SimulacaoServices simulacaoService;

	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;

	@Before
	public void setup() {
		simuladorTaxasController = new SimuladorTaxasController(calculadoraService, simulacaoService, false, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
	}
	
	
	//ComAcessorios
	//ComValorSubsidio
	@Test
	public void getSimulacaoTaxaComAcessorios() {

		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);

		simuladorTaxasController.getParcelaTaxas(request, null);
	}
	
	//SemAcessorios
	@Test
	public void getSimulacaoSemAcessorios() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.setValorAcessorios(null);
		simuladorTaxasController.getParcelaTaxas(request, null);
	}
	
	//ComValorSubsidio
	//ComSeguroAuto
	@Test
	public void getSimulacaoComValorSubsidio() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.getSubsidio().setTaxaSubsidio(null);
		request.getSubsidio().setValorSubsidio(new BigDecimal(1200));
		request.setValorSeguroAuto(new BigDecimal(800));
		simuladorTaxasController.getParcelaTaxas(request, null);
	}
	
	//SemSubsidio
	//ComSeguroPrestamista
	@Test
	public void getSimulacaoSemSubsidio() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.getSubsidio().setTaxaSubsidio(null);
		request.getSubsidio().setValorSubsidio(null);
		request.setValorSeguroPrestamista(new BigDecimal(800));
		simuladorTaxasController.getParcelaTaxas(request, null);
		
		request.setSubsidio(null);
		simuladorTaxasController.getParcelaTaxas(request, null);
	}	
	
	//SemAcessorios
	@Test
	public void getSimulacaoSemAcessoriosBalao() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.setValorAcessorios(null);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
		ParcelaBalaoRequest balao = new ParcelaBalaoRequest();
		balao.setMesAnoVencimento(request.getDataPrimeiroVencimento().plusMonths(request.getPrazo()).format(formatter));
		balao.setValor(new BigDecimal(30000));
		
		List<ParcelaBalaoRequest> parcelasBalao = new ArrayList<>();
		parcelasBalao.add(balao);
		
		request.setParcelasBalao(parcelasBalao);
		
		simuladorTaxasController.getParcelaTaxas(request, null);
	}
	
	//ComValorSubsidio
	//ComSeguroAuto
	@Test
	public void getSimulacaoComValorSubsidioBalao() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.getSubsidio().setTaxaSubsidio(null);
		request.getSubsidio().setValorSubsidio(new BigDecimal(1200));
		request.setValorSeguroAuto(new BigDecimal(800));

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
		ParcelaBalaoRequest balao = new ParcelaBalaoRequest();
		balao.setMesAnoVencimento(request.getDataPrimeiroVencimento().plusMonths(request.getPrazo()).format(formatter));
		balao.setValor(new BigDecimal(30000));
		
		ParcelaBalaoRequest balao2 = new ParcelaBalaoRequest();
		balao2.setMesAnoVencimento(request.getDataPrimeiroVencimento().plusMonths(2).format(formatter));
		balao2.setValor(new BigDecimal(30000));
		
		List<ParcelaBalaoRequest> parcelasBalao = new ArrayList<>();
		parcelasBalao.add(balao);
		parcelasBalao.add(balao2);
		
		request.setParcelasBalao(parcelasBalao);		
		
		simuladorTaxasController.getParcelaTaxas(request, null);
	}
	
	//SemSubsidio
	//ComSeguroPrestamista
	@Test
	public void getSimulacaoSemSubsidioBalao() {
		SimulacaoTaxaRequest request = criarRequest(24, new BigDecimal(125000), new BigDecimal(60000), false, 1);
		request.getSubsidio().setTaxaSubsidio(null);
		request.getSubsidio().setValorSubsidio(null);
		request.setValorSeguroPrestamista(new BigDecimal(800));
		simuladorTaxasController.getParcelaTaxas(request, null);
		
		request.setSubsidio(null);
		simuladorTaxasController.getParcelaTaxas(request, null);
	}		
	
	
	private SimulacaoTaxaRequest criarRequest(Integer prazo, BigDecimal valorBem, BigDecimal valorEntrada, Boolean informaPrimeiroVencimento, Integer periodicidade) {
		SimulacaoTaxaRequest request = new SimulacaoTaxaRequest();
		request.setValorBem(valorBem);
		request.setValorEntrada(valorEntrada);
		request.setInformaPrimeiroVencimento(informaPrimeiroVencimento);
		request.setPeriodicidade(periodicidade);
		request.setDataPrimeiroVencimento(LocalDate.now().plusMonths(periodicidade));
		request.setPrazo(prazo);
		request.setIdTaxa(null);
		request.setAliquitaIof(new BigDecimal(0.03));
		request.setAliquitaIofAdicional(new BigDecimal(0.0038));
		request.setCodigoRetorno("2");
		request.setDataInicio(LocalDate.now());
		request.setTaxaBanco(new BigDecimal(1.43));
		request.setValorAcessorios(new BigDecimal(750));
		request.setValorSeguroAuto(null);
		request.setValorSeguroPrestamista(null);
		request.setValorTarifa(new BigDecimal(444));
		request.setValorUfEmplacamento(new BigDecimal(555));
		
		request.setParcelasBalao(null);
		request.setSubsidio(new SubsidioRequest(new BigDecimal(0.1), null));
		
		return request;

	}

}
