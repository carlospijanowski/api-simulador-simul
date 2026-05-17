package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.Times;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SimuladorParcResidualControllerTest {

	public static final BigDecimal OITO_MIL = new BigDecimal(8000);
	public static final BigDecimal DEZ_MIL = new BigDecimal(10000);
	private SimuladorParcResidualController controller;

	private CalculadoraServices calculadoraServices = new CalculadoraServicesImpl();

	@MockBean
	private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

	@MockBean
	private DataBARepository repository;

	@MockBean
	private RedisRepository redisRepository;

	private DataCarencia dataCarencia;

	@MockBean
	private SimulacaoServices simulacaoServices;

	@MockBean
	private ParametrosDeSeguroPrestamistaService params;

	@MockBean
	private LogService logService;
	@MockBean
	private CalculadoraSeguroFranquiaServices seguroFranquiaServices;
	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;

	@MockBean
	private FeatureToggleConfig featureToggleConfig;

	@MockBean
    private MotorTaxaPlanoService motorTaxaPlanoService;

	@MockBean
	private FatorSimulacao fatorSimulacao;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);

		dataCarencia = new DataCarenciaImpl(repository, redisRepository);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
		controller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, params, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);


		ReflectionTestUtils.setField(controller, "motorTaxaPlanoService", motorTaxaPlanoService);
		doNothing().when(motorTaxaPlanoService).applyFatorRating(any());
		controller.setLogService(logService);

	}

	@Test(expected = ServicoForaException.class)
	public void testeSemSubsidioESemRetornoComErroClientesV2() {
		when(redisRepository.getDataBA()).thenThrow(new ServicoForaException("Erro clientes v2", null));
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setRetorno(null);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("Campo obrigatório para plano sem subsídio", ex.getMessage());
		}
	}

	@Test
	public void testeSemSubsidioESemRetorno() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setRetorno(null);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("Campo obrigatório para plano sem subsídio", ex.getMessage());
		}
	}

	@Test
	public void testeValorBemZero() throws JsonProcessingException {
		String str = "{\n" + "  \"idSimulacao\" : \"FATOR_PARA_VALOR_NA_PARCELA\",\n"
				+ "  \"dataSimulacao\" : [ 2019, 2, 18 ],\n" + "  \"dataPrimeiroVencimento\" : [ 2019, 6, 22 ],\n"
				+ "  \"valorBem\" : 0,\n" + "  \"valorEntrada\" : 0,\n" + "  \"valorSeguroPrestamista\" : 0,\n"
				+ "  \"custoPercentualDoSeguroPrestamista\" : null,\n" + "  \"valorRegistroContrato\" : 0,\n"
				+ "  \"valorCestaServico\" : 0,\n" + "  \"valorTC\" : 0,\n" + "  \"valorTaxaSubsidio\" : null,\n"
				+ "  \"codigoRetorno\" : \"1\",\n" + "  \"periodicidade\" : 1,\n"
				+ "  \"tipoFinanciamento\" : \"CDC\",\n" + "  \"tipoCalculo\" : \"TAXA\",\n"
				+ "  \"valorSubsidio\" : null,\n" + "  \"temIsencaoIOF\" : false,\n" + "  \"taxaIOFAA\" : 0.030000,\n"
				+ "  \"taxaIOFAD\" : 0.003800,\n" + "  \"baseCalculoIOF\" : 365,\n" + "  \"parcelasBalao\" : [ ],\n"
				+ "  \"prazo\" : {\n" + "    \"prazo\" : 12,\n" + "    \"taxa-mes\" : 1.308033,\n"
				+ "    \"perc-min-balao\" : 20.00,\n" + "    \"perc-max-balao\" : 50.00,\n"
				+ "    \"perc-min-entrada\" : 20,\n" + "    \"perc-max-entrada\" : 50\n" + "  },\n"
				+ "  \"tipoPessoa\" : \"pessoa-fisica\",\n" + "  \"itens\" : null\n" + "}";

		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();

		DadosSimulacao dadosSimulacao = mapper.readValue(str, DadosSimulacao.class);
		dadosSimulacao.setUsuarioInformouDataPrimeiroVencimento(true);

		// sem o tratamento do valor do bem zero vai dar o seguinte erro:
		// java.lang.ClassCastException: org.apache.poi.ss.formula.eval.ErrorEval cannot
		// be cast to org.apache.poi.ss.formula.eval.NumberEval
		try {
			calculadoraServices.calcular(dadosSimulacao);
		} catch (ClassCastException ex) {
			fail("class cast exception");
		}
	}

	@Test
	public void testeComPlanoDeTaxaFixa() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.FIXO)
				.taxaBanco(BigDecimal.ONE).fixoPercentualMontadora(new BigDecimal(50))
				.fixoPercentualRevendedora(new BigDecimal(50)).build());
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorSubsidio(BigDecimal.TEN);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado é do tipo subsídio fixo", ex.getMessage());
		}

		request.setValorSubsidio(null);
		request.setTaxaSubsidio(BigDecimal.ONE);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado é do tipo subsídio fixo", ex.getMessage());
		}

		request.setTaxaSubsidio(null);
		SimulacaoParcResidualResponse response = controller.getParcelaResidual(request).getBody();
		assertEquals(3, response.getPrazos().size());
		response.getPrazos().forEach(p -> assertNotNull(p.getSubsidio()));
	}

	@Test
	public void testeComPlanoSemSubsidio() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorSubsidio(BigDecimal.TEN);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			ex.printStackTrace();
			assertEquals("plano selecionado não possui subsídio", ex.getMessage());
		}

		request.setValorSubsidio(null);
		request.setTaxaSubsidio(BigDecimal.ONE);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado não possui subsídio", ex.getMessage());
		}
	}

	/**
	 * Verifica que as datas dos pagamentos estão realmente respeitando a
	 * periodicidade passada
	 */
	@Test
	public void testeComPeriodicidadeBimestral() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		// dada do primeiro pagamento já é passada
		request.setPeriodicidade(2);

		LocalDate primeiroVencimento = request.getDataPrimeiroVencimento();
		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, true, dataCarencia.getDataCalculo(),false);
		Iterator<ResultadoCalculadoParcelaResidual> i = result.getList().iterator();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);
		while (i.hasNext()) {
			LocalDate referencia = primeiroVencimento;
			ResultadoCalculadoParcelaResidual p = i.next();
			// pula a entrada que vem em primeiro
			p.getFluxo().remove(0);
			for (Fluxo fluxo : p.getFluxo()) {
				LocalDate data = LocalDate.parse(fluxo.getDataParcela(), formatter);
				assertEquals(referencia, data);
				referencia = referencia.plusMonths(request.getPeriodicidade());
			}
		}
	}

	/**
	 * Verifica que as datas dos pagamentos estão realmente respeitando a
	 * periodicidade passada
	 */
	@Test
	public void testeComPeriodicidadeBimestralSemCarencia() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.getCarenciaDoPlano().setMaximo(Constants.ZERO_INTEIRO);
		request.getCarenciaDoPlano().setMinimo(Constants.ZERO_INTEIRO);
		// dada do primeiro pagamento já é passada
		request.setPeriodicidade(2);

		LocalDate primeiroVencimento = request.getDataPrimeiroVencimento();
		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, true, dataCarencia.getDataCalculo(),false);
		Iterator<ResultadoCalculadoParcelaResidual> i = result.getList().iterator();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);
		while (i.hasNext()) {
			LocalDate referencia = primeiroVencimento;
			ResultadoCalculadoParcelaResidual p = i.next();
			// pula a entrada que vem em primeiro
			p.getFluxo().remove(0);
			for (Fluxo fluxo : p.getFluxo()) {
				LocalDate data = LocalDate.parse(fluxo.getDataParcela(), formatter);
				assertEquals(referencia, data);
				referencia = referencia.plusMonths(request.getPeriodicidade());
			}
		}
	}

	@Test
	public void testeComPeriodicidadeBimestralComPrimeiroPagamentoEm60Dias() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setNDiasAposVencimento(true);
		request.setPeriodicidade(2);

		LocalDate primeiroVencimento = repository.findAll().get(0).getAtual().plusMonths(request.getPeriodicidade());

		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, true, dataCarencia.getDataCalculo(),false);
		Iterator<ResultadoCalculadoParcelaResidual> i = result.getList().iterator();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.FORMATO_DATA_BRASIL);
		while (i.hasNext()) {
			LocalDate referencia = primeiroVencimento;
			ResultadoCalculadoParcelaResidual p = i.next();
			// remove a entrada que vem em primeiro
			p.getFluxo().remove(0);
			for (Fluxo fluxo : p.getFluxo()) {
				LocalDate data = LocalDate.parse(fluxo.getDataParcela(), formatter);
				assertEquals(referencia, data);
				referencia = referencia.plusMonths(request.getPeriodicidade());
			}
		}
	}

	@Test
	public void testeComZeroIntermediariasPlanoCDC() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 12 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CDC,true, EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);

		List<BigDecimal> valorDaParcela = new ArrayList<>(parcelas.length);
		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, false,
				dataCarencia.getDataCalculo(),false);
		result.getList().forEach(r -> {
			valorDaParcela.add(r.getValorParcela().setScale(2, RoundingMode.HALF_UP));
		});

		ResultadoParcelaResidual resultFluxo = controller.fazerSimulacao(request, true, true,
				dataCarencia.getDataCalculo(),false);
		for (int i = 0; i < resultFluxo.getList().size(); i++) {
			ResultadoCalculadoParcelaResidual r = resultFluxo.getList().get(i);
			BigDecimal parcela = valorDaParcela.get(i);
			for (int j = 1; j < r.getFluxo().size(); j++) {
				Fluxo f = r.getFluxo().get(j);
				// sem intermediárias o valor da parcela é igual para todas as parcelas
				assertTrue("parcelas devem ter o valor de " + parcela, f.getValorParcela().compareTo(parcela) == 0);
			}
		}
	}

	@Test
	public void testeComDuasIntermediariasPlanoCDCComUmPrazos() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 36 };
		assertEquals(1, prazoList.size());
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CDC,true, EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);
		List<ParcelaIntermediaria> intermediarias = new ArrayList<>();

		LocalDate dataCalculo = repository.findAll().get(0).getAtual();

		System.out.println("data de cálculo: " + dataCalculo);
		System.out.println("data primeiro pagamento: " + request.getDataPrimeiroVencimento());
		System.out.println("30 dias após formalização: " + request.getTrintaDiasAposVencimento());

		criarIntermediaria(OITO_MIL, dataCalculo.plusMonths(5), intermediarias, dataCalculo);
		criarIntermediaria(DEZ_MIL, dataCalculo.plusMonths(12), intermediarias, dataCalculo);
		request.setIntermediarias(intermediarias);

		List<BigDecimal> valorDaParcela = new ArrayList<>(parcelas.length);
		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, false,
				dataCarencia.getDataCalculo(),false);
		result.getList().forEach(r -> {
			valorDaParcela.add(r.getValorParcela().setScale(2, RoundingMode.HALF_UP));
		});

		ResultadoParcelaResidual resultFluxo = controller.fazerSimulacao(request, true, true,
				dataCarencia.getDataCalculo(),false);
		for (int i = 0; i < resultFluxo.getList().size(); i++) {
			ResultadoCalculadoParcelaResidual r = resultFluxo.getList().get(i);
			BigDecimal parcela = valorDaParcela.get(i);
			for (int j = 0; j < r.getFluxo().size(); j++) {
				Fluxo f = r.getFluxo().get(j);
				if (j == 2) {
					assertTrue("segunda parcela deve ter uma intermediária de 8 mil",
							OITO_MIL.compareTo(f.getValorIntermediaria()) == 0);
				} else if (j == 9) {
					assertTrue("nona parcela deve ter uma intermediária de 10 mil",
							DEZ_MIL.compareTo(f.getValorIntermediaria()) == 0);
				} else if (j == 0) {
					assertTrue("outras parcelas devem ter o valor de " + parcela,
							f.getValorParcela().compareTo(request.getValorEntrada()) == 0);
				} else {
					assertTrue("outras parcelas devem ter o valor de " + parcela,
							f.getValorParcela().compareTo(parcela) == 0);
				}
			}
		}
	}

	@Test(expected = BusinessValidationException.class)
	public void testeComParcelaResidualPlanoCDCComUmPrazos() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 36 };
		assertEquals(1, prazoList.size());
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CDC);

		LocalDate dataCalculo = repository.findAll().get(0).getAtual();

		System.out.println("data de cálculo: " + dataCalculo);
		System.out.println("data primeiro pagamento: " + request.getDataPrimeiroVencimento());
		System.out.println("30 dias após formalização: " + request.getTrintaDiasAposVencimento());
		List<BigDecimal> valorDaParcela = new ArrayList<>(parcelas.length);
		ResultadoParcelaResidual result = controller.fazerSimulacao(request, true, false,
				dataCarencia.getDataCalculo(),false);
		result.getList().forEach(r -> {
			valorDaParcela.add(r.getValorParcela());
		});

		ResultadoParcelaResidual resultFluxo = controller.fazerSimulacao(request, true, true,
				dataCarencia.getDataCalculo(),false);
		for (int i = 0; i < resultFluxo.getList().size(); i++) {
			ResultadoCalculadoParcelaResidual r = resultFluxo.getList().get(i);
			BigDecimal parcela = valorDaParcela.get(i);
			for (int j = 0; j < r.getFluxo().size(); j++) {
				Fluxo f = r.getFluxo().get(j);
				if (j == 2) {
					assertTrue("segunda parcela deve ter uma intermediária de 8 mil",
							OITO_MIL.compareTo(f.getValorIntermediaria()) == 0);
				} else if (j == 9) {
					assertTrue("nona parcela deve ter uma intermediária de 10 mil",
							DEZ_MIL.compareTo(f.getValorIntermediaria()) == 0);
				} else if (j == 0) {
					assertTrue("outras parcelas devem ter o valor de " + parcela,
							f.getValorParcela().compareTo(request.getValorEntrada()) == 0);
				} else {
					assertTrue("outras parcelas devem ter o valor de " + parcela,
							f.getValorParcela().compareTo(parcela) == 0);
				}
			}
		}
	}

	@Test
	public void testarLimiteInferiorData() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 24 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CDC,true , EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);
		List<ParcelaIntermediaria> intermediarias = new ArrayList<>();

		LocalDate dataCalculo = repository.findAll().get(0).getAtual();

		System.out.println("data de cálculo: " + dataCalculo);
		System.out.println("data primeiro pagamento: " + request.getDataPrimeiroVencimento());
		System.out.println("30 dias após formalização: " + request.getTrintaDiasAposVencimento());

		criarIntermediaria(OITO_MIL, dataCalculo.plusMonths(-2), intermediarias, dataCalculo);
		request.setIntermediarias(intermediarias);

		try {
			controller.fazerSimulacao(request, true, false, dataCarencia.getDataCalculo(),false);
			fail("a data da intermediária é inválida e isso tem que ser verificado");
		} catch (BusinessValidationException ex) {
			assertEquals(
					"Data 2018-12 está fora do intervalo de validade do financiamento: 2019-06 até 2020-06 para prazo de 12 parcelas",
					ex.getMessage());
		}
	}

	@Test
	public void testarLimiteSuperiorData() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		Integer[] parcelas = { 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CDC,true, EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);
		List<ParcelaIntermediaria> intermediarias = new ArrayList<>();

		LocalDate dataCalculo = repository.findAll().get(0).getAtual();

		System.out.println("data de cálculo: " + dataCalculo);
		System.out.println("data primeiro pagamento: " + request.getDataPrimeiroVencimento());
		System.out.println("30 dias após formalização: " + request.getTrintaDiasAposVencimento());

		criarIntermediaria(OITO_MIL, dataCalculo.plusMonths(20), intermediarias, dataCalculo);
		request.setIntermediarias(intermediarias);

		try {
			controller.fazerSimulacao(request, true, false, dataCarencia.getDataCalculo(),false);
			fail("a data da intermediária é inválida e isso tem que ser verificado");
		} catch (BusinessValidationException ex) {
			assertEquals(
					"Data 2020-10 está fora do intervalo de validade do financiamento: 2019-06 até 2020-06 para prazo de 12 parcelas",
					ex.getMessage());
		}
	}

	private void criarIntermediaria(BigDecimal valor, LocalDate data, List<ParcelaIntermediaria> intermediarias,
			LocalDate dataCalculo) {
		ParcelaIntermediaria pi1 = new ParcelaIntermediaria();
		pi1.setValor(valor);
		pi1.setVencimento(data);
		intermediarias.add(pi1);
	}

	private void mockSubsidioVariavel() {
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(Subsidio.builder()
				.tipoSubsidio(EnumTipoSubsidio.VARIAVEL).tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL)
				.vpPercentualDistrMarca(new BigDecimal(100)).vpValorMaxMarca(new BigDecimal(9999999)).build());
	}

	@Test
	public void testeComValorDoSubsidio() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		mockSubsidioVariavel();
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorSubsidio(new BigDecimal("-1"));

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertTrue(ex.getMessage().startsWith("O valor do subsídio é de R$ 10 a "));
		}

		request.setValorSubsidio(new BigDecimal(100000));

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertTrue(ex.getMessage().startsWith("O valor do subsídio é de R$ 10 a "));
		}

		request.setValorSubsidio(new BigDecimal(1000));
		SimulacaoParcResidualResponse response = controller.getParcelaResidual(request).getBody();
		assertEquals(3, response.getPrazos().size());
		response.getPrazos().forEach(p -> assertNotNull(p.getSubsidio()));
	}

	@Test(expected = HttpMessageConversionException.class)
	public void testeComValorDoSubsidioComToken() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		mockSubsidioVariavel();
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setCpfProponente("");
		request.setValorSubsidio(new BigDecimal("-1"));

		try {
			controller.getParcelaResidual("token", request);
			fail();
		} catch (BusinessValidationException ex) {
			assertTrue(ex.getMessage().startsWith("valor fora do limite de 10 até"));
		}
	}

	@Test
	@Ignore
	public void testeComValorETaxaDoSubsidio() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt()))
				.thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.VARIAVEL).build());
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorSubsidio(new BigDecimal("20000000"));
		request.setTaxaSubsidio(new BigDecimal("1"));

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("O valor do subsídio é de R$ 10 a R$ " + NumberUtils.format(new BigDecimal("16398.33")), ex.getMessage());
		}
	}

	@Test
	public void testeSemValorETaxaDoSubsidio() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt()))
				.thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.VARIAVEL).build());
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		try {
			controller.getParcelaResidual(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano com subsídio requer valor-subsidio ou taxa-subsidio", ex.getMessage());
		}
	}

	@Test
	public void testeTaxaMaximaDoSubsidio() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt()))
				.thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.FIXO).build());
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		try {
			controller.getTaxaMaximaDoSubsidio(2752, new BigDecimal("30"), null);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
		}

		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		try {
			controller.getTaxaMaximaDoSubsidio(2752, new BigDecimal("30"), null);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
		}
	}

	@Test
	public void testeTaxaMaximaDoSubsidioPassandoValor() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		when(simulacaoServices.getSubsidio(anyInt()))
				.thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.FIXO).build());
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		try {
			controller.getTaxaMaximaDoSubsidio(2752, new BigDecimal("30"), 36);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
		}

		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		try {
			controller.getTaxaMaximaDoSubsidio(2752, new BigDecimal("30"), 36);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
		}
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualNegativa() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorEntrada(request.getValorBem().subtract(request.getValorParcelaResidual()).add(BigDecimal.TEN));

		controller.getParcelaResidual(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualTaxaDeSubsidioMuitoAlta() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		// não pode ser maior que a maior taxa banco - 0.01
		request.setTaxaSubsidio(new BigDecimal("2.0"));

		controller.getParcelaResidual(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualTaxaDeSubsidioNegativa() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setTaxaSubsidio(new BigDecimal("-1.0"));

		controller.getParcelaResidual(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualValorDeSubsidioMuitoAlta() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		// não pode ser maior que a maior taxa banco - 0.01
		request.setValorSubsidio(new BigDecimal("14603.84").add(new BigDecimal("0.01")));

		controller.getParcelaResidual(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualValorDeSubsidioMuitoBaixa() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorSubsidio(BigDecimal.ONE);

		controller.getParcelaResidual(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void getParcelaResidualSubsidioPorTaxaEPorValor() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setTaxaSubsidio(BigDecimal.ONE);
		request.setValorSubsidio(BigDecimal.TEN);

		controller.getParcelaResidual(request);
	}

	@Test
	public void getParcelaResidualTaxaDeSubsidioOK() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		mockSubsidioVariavel();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		// não pode ser maior que a maior taxa banco - 0.01
		request.setTaxaSubsidio(new BigDecimal("1.308033").subtract(new BigDecimal("0.01")));

		controller.getParcelaResidual(request);
	}

	@Test
	public void getParcelaResidualForaDoLimiteInferior() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorParcelaResidual(BigDecimal.TEN); // menor que 20% pra passar do limite interior
		request.setValorCestaServicos(BigDecimal.ZERO);
		request.setValorTC(BigDecimal.ZERO);
		request.setValorUfEmplacamento(BigDecimal.ZERO);
		request.setValorEntrada(request.getValorBem().subtract(request.getValorParcelaResidual()));

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		System.out.println(simulacao);

		List<PrazoResidualResponse> list = simulacao.getPrazos();

		assertFalse(list.isEmpty());

		List<BigDecimal> valoresSemOsItens = new ArrayList<>();
		for (PrazoResidualResponse r : list) {
			System.out.println("parcela: " + r.getValorParcela() + " prazo: " + r.getParcelas() + " residual: "
					+ r.getValorParcelaResidual());
			assertTrue(
					"valor da parcela residual não pode ser igual ao valor solicitado pois esta fora do limite inferior",
					request.getValorParcelaResidual().compareTo(r.getValorParcelaResidual()) < 0);
		}
	}

	@Test
	public void getParcelaResidualForaDoLimiteSuperior() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setValorParcelaResidual(new BigDecimal(70000)); // maior que 50% pra passar do limite superior
		request.setValorCestaServicos(BigDecimal.ZERO);
		request.setValorTC(BigDecimal.ZERO);
		request.setValorUfEmplacamento(BigDecimal.ZERO);
		request.setValorEntrada(request.getValorBem().subtract(request.getValorParcelaResidual()));

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		System.out.println(simulacao);

		List<PrazoResidualResponse> list = simulacao.getPrazos();

		assertFalse(list.isEmpty());

		List<BigDecimal> valoresSemOsItens = new ArrayList<>();
		for (PrazoResidualResponse r : list) {
			System.out.println("parcela: " + r.getValorParcela() + " prazo: " + r.getParcelas() + " residual: "
					+ r.getValorParcelaResidual());
			assertTrue(
					"valor da parcela residual não pode ser igual ao valor solicitado pois esta fora do limite superior",
					request.getValorParcelaResidual().compareTo(r.getValorParcelaResidual()) > 0);
		}
	}

	@Test
	public void testaValorDoSeguro() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true);

		Integer[] parcelas = { 12 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();

		List<PrazoResidualResponse> list = simulacao.getPrazos();
		assertEquals(1, list.size());
		PrazoResidualResponse r = list.get(0);
		BigDecimal valorDoSeguro = r.getValorDoSeguroPrestamista();

		List<ItemFinanciavel> itens = new ArrayList<>();
		itens.add(getItemFinanciavel(1, new BigDecimal(1000)));
		request.setItens(itens);

		response = controller.getParcelaResidual(request);
		simulacao = response.getBody();
		list = simulacao.getPrazos();
		assertEquals(1, list.size());
		r = list.get(0);
		BigDecimal valorDoSeguroComItem = r.getValorDoSeguroPrestamista();

		assertTrue("valor do seguro com item financiavel tem que ser maior que sem o item",
				valorDoSeguroComItem.compareTo(valorDoSeguro) > 0);
	}

	@Test
	public void testaValorDoSeguroGarantia() throws JsonProcessingException {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true);
		ParametrosSeguroMecanica parametrosSeguroMecanica = new ParametrosSeguroMecanica();
		parametrosSeguroMecanica.setCnpjOrigemNegocio("12345678901234");
		parametrosSeguroMecanica.setAnoFabricacao(2022);
		parametrosSeguroMecanica.setDataInicioSeguroMecanica(LocalDate.now());
		parametrosSeguroMecanica.setPossuiSeguroMecanica(true);
		parametrosSeguroMecanica.setMarca("Toyota");
		parametrosSeguroMecanica.setAnoModelo("2023");
		parametrosSeguroMecanica.setCodigoFipe("002207127");
		parametrosSeguroMecanica.setModelo("YARIS XL PLUS CON. SED. 1.5 FLEX 16V AUT");
		parametrosSeguroMecanica.setQuilometragem("0");
		Integer[] parcelas = { 12 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);
		request.setParametrosSeguroMecanica(parametrosSeguroMecanica);
		CalculoSeguroMecanicaResponse calculoSeguroMecanicaResponse = new CalculoSeguroMecanicaResponse();
		SeguroMecanica seguroMecanica = new SeguroMecanica();
		seguroMecanica.setValorParcela(new BigDecimal(2000));
		seguroMecanica.setPrazo(12);
		calculoSeguroMecanicaResponse.setCotacoes(Collections.singletonList(seguroMecanica));
		when(calculadoraSeguroMecanicaServices.calcular(Mockito.any(), Mockito.any())).thenReturn(calculoSeguroMecanicaResponse);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		response = controller.getParcelaResidual(request);
		simulacao = response.getBody();
		assertEquals(1, simulacao.getPrazos().get(0).getDadosSeguroMecanica().getCotacoes().size());


	}

	@Test
	public void testaValorDoPrestamistaComSeguroAuto() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true);

		Integer[] parcelas = { 12 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();

		List<PrazoResidualResponse> list = simulacao.getPrazos();
		assertEquals(1, list.size());
		PrazoResidualResponse r = list.get(0);
		BigDecimal valorDoPrestamistaSemSeguroAuto = r.getValorDoSeguroPrestamista();
		TipoDeSeguroPrestamista tipoPrestamistaSemSeguro = r.getTipoDeSeguroPrestamista();

		request.setSeguroAuto(new BigDecimal(3000));

		response = controller.getParcelaResidual(request);
		simulacao = response.getBody();
		list = simulacao.getPrazos();
		assertEquals(1, list.size());
		r = list.get(0);
		BigDecimal valorDoPrestamistaComSeguroAuto = r.getValorDoSeguroPrestamista();
		TipoDeSeguroPrestamista tipoPrestamistaComSeguro = r.getTipoDeSeguroPrestamista();
		if (tipoPrestamistaSemSeguro.equals(tipoPrestamistaComSeguro)) {
			assertTrue("valor do prestamista com seguro auto tem que ser maior que sem o seguro auto",
					valorDoPrestamistaComSeguroAuto.compareTo(valorDoPrestamistaSemSeguroAuto) > 0);
		} else {
			assertTrue("valor do prestamista com seguro auto tem que ser menor que sem o seguro auto",
					valorDoPrestamistaComSeguroAuto.compareTo(valorDoPrestamistaSemSeguroAuto) < 0);
		}
	}

	@Test
	public void getParcelaResidual() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, null);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		System.out.println(simulacao);

		List<PrazoResidualResponse> list = simulacao.getPrazos();

		assertFalse(list.isEmpty());

		List<BigDecimal> valoresSemOsItens = new ArrayList<>();
		for (PrazoResidualResponse r : list) {
			valoresSemOsItens.add(r.getValorParcela());
			assertEquals(TipoDeSeguroPrestamista.SPF, r.getTipoDeSeguroPrestamista());

			/*
			 * Valida se a parcela da residual não ultrapassa a porcentagem maxima da
			 * residual definida no redis
			 */
			prazoList.stream().filter(Objects::nonNull).forEach(p -> {
				BigDecimal maximoResidual = request.getValorBem().multiply(
						p.getPercentualMaxBalao().divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP));
				assertTrue(r.getValorParcelaResidual().compareTo(maximoResidual) <= 0);
			});
		}

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);

		List<ItemFinanciavel> items = new ArrayList<>();

		items.add(getItemFinanciavel(1, new BigDecimal(100)));
		items.add(getItemFinanciavel(2, new BigDecimal(200)));
		request.setItens(items);

		response = controller.getParcelaResidual(request);
		simulacao = response.getBody();
		list = simulacao.getPrazos();
		List<BigDecimal> valoresComOsItens = new ArrayList<>();
		for (PrazoResidualResponse r : list) {
			valoresComOsItens.add(r.getValorParcela());
			assertEquals(TipoDeSeguroPrestamista.SPF, r.getTipoDeSeguroPrestamista());
		}
		assertEquals(valoresSemOsItens.size(), valoresComOsItens.size());

		for (int i = 0; i < valoresComOsItens.size(); i++) {
			assertTrue(valoresComOsItens.get(i).compareTo(valoresSemOsItens.get(i)) > 0);
		}
	}

	@Test
	public void getParcelaResidualComRecalculoDoSeguro() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 120850.00, null);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		BigDecimal valorDoSeguroSPF = null;
		BigDecimal valorDoSeguroSVP = null;
		for (PrazoResidualResponse r : list) {
			if (r.getParcelas() == 12) {
				valorDoSeguroSVP = r.getValorDoSeguroPrestamista();
				assertEquals(TipoDeSeguroPrestamista.SVP, r.getTipoDeSeguroPrestamista());
			} else {
				if (valorDoSeguroSPF == null) {
					valorDoSeguroSPF = r.getValorDoSeguroPrestamista().add(request.getSeguroAuto());
				} else {
					// o valor do seguro deve ser o mesmo independente do número de parcelas
					assertEquals(valorDoSeguroSPF, r.getValorDoSeguroPrestamista().add(request.getSeguroAuto()));
				}
				assertEquals(TipoDeSeguroPrestamista.SPF, r.getTipoDeSeguroPrestamista());
			}

			assertNotNull("valor do seguro deve ser retornado", r.getValorDoSeguroPrestamista());
			assertNotNull("valor do seguro deve ser retornado", r.getValorDoSeguroPrestamistaNaParcela());
			assertTrue("valor do seguro deve ser maior que zero",
					r.getValorDoSeguroPrestamista().compareTo(BigDecimal.ZERO) > 0);
			assertTrue("valor do seguro na parcela deve ser maior que zero",
					r.getValorDoSeguroPrestamistaNaParcela().compareTo(BigDecimal.ZERO) > 0);
			assertTrue(" valor na parcela deve ser menor que valor total",
					r.getValorDoSeguroPrestamista().compareTo(r.getValorDoSeguroPrestamistaNaParcela()) > 0);
			assertEquals("valor do seguro deve ter duas casas decimais", 2, r.getValorDoSeguroPrestamista().scale());
			assertEquals("valor do seguro deve ter duas casas decimais", 2,
					r.getValorDoSeguroPrestamistaNaParcela().scale());
		}

		assertTrue("valor do seguro SVP deve ser menor que do SPF", valorDoSeguroSVP.compareTo(valorDoSeguroSPF) < 0);

		List<PrazoResidualResponse> list2 = new ArrayList<>(list);
		list2.sort(Comparator.comparingInt(PrazoResidualResponse::getParcelas));
		assertEquals("lista deve estar ordenada", list, list2);

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	@Test
	public void getParcelaResidualSemSeguro() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 460850.00, null);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	@Test
	public void getParcelaResidualSemSeguroEstourandoLimite() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, null);
		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	@Test
	public void testeDeMemoria() {
		Runtime runtime = Runtime.getRuntime();
		for (int i = 0; i < 10; i++) {
			getCalculoSeguroAuto();
		}
		CalculadoraServicesImpl.setListParaTesteDeMemoria(new ArrayList<>());
		for (int j = 1; j < 10; j++) {
			System.gc();
			long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
			for (int i = 0; i < j; i++) {
				getCalculoSeguroAuto();
			}
			System.gc();
			long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
			System.out.println("memória com " + j + " simulações: " + (memoryAfter - memoryBefore));
			CalculadoraServicesImpl.setListParaTesteDeMemoria(new ArrayList<>());
		}
		CalculadoraServicesImpl.setListParaTesteDeMemoria(null);
	}

	@Test
	public void getCalculoSeguroAuto() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 160850.00, Modalidade.CICLO_TOYOTA,true,EnumControleBalao.OBRIGATORIO_BALAO);
		request.setSeguroAuto(new BigDecimal(2500));
		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> assertTrue("seguro auto deve ser calculado na parcela",
				BigDecimal.ZERO.compareTo(r.getValorDoSeguroAutoNaParcela()) != 0));
	}

	private List<Prazo> prepararMockDeSimulacaoServices() {
		return prepararMockDeSimulacaoServices(false);
	}

	private List<Prazo> prepararMockDeSimulacaoServices(boolean soUmaParcela) {
		return prepararMockDeSimulacaoServices(soUmaParcela, false);
	}

	private List<Prazo> prepararMockDeSimulacaoServices(boolean soUmaParcela, boolean cdc) {
		// { "base-calculo": "365.000000", "iof-aa": "0.030000", "iof-ad": "0.003800" }
		TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");

		BigDecimal limiteResidualMin[] = new BigDecimal[3];
		BigDecimal limiteResidualMax[] = new BigDecimal[3];
		if (cdc) {
			// no back consideramos que planos CDC tem os limites do residual igual a zero,
			// se não for o caso teremos que rever o código
			// e ver como fazer isso de outra forma
			for (int i = 0; i < limiteResidualMin.length; i++) {
				limiteResidualMax[i] = BigDecimal.ZERO;
				limiteResidualMin[i] = BigDecimal.ZERO;
			}
		} else {
			limiteResidualMin[0] = new BigDecimal("20.00");
			limiteResidualMax[0] = new BigDecimal("50.00");
			limiteResidualMin[1] = new BigDecimal("20.00");
			limiteResidualMax[1] = new BigDecimal("50.00");
			limiteResidualMin[2] = new BigDecimal("20.00");
			limiteResidualMax[2] = new BigDecimal("40.00");
		}

		// plano 2619
		// { "prazo": "12", "taxa-mes": "1.308033", "perc-min-balao": "20.00",
		// "perc-max-balao": "50.00" }
		// { "prazo": "24", "taxa-mes": "1.308033", "perc-min-balao": "20.00",
		// "perc-max-balao": "50.00" }
		// { "prazo": "36", "taxa-mes": "1.308033", "perc-min-balao": "20.00",
		// "perc-max-balao": "40.00" }
		List<Prazo> prazoList = new ArrayList<>();
		BigDecimal limiteDeFinanciamento = new BigDecimal(330000);
		prazoList.add(new Prazo(12, new BigDecimal("1.308033"), limiteResidualMin[0], limiteResidualMax[0],
				new BigDecimal(20), new BigDecimal(50),null));
		if (!soUmaParcela) {
			prazoList.add(new Prazo(24, new BigDecimal("1.308033"), limiteResidualMin[1], limiteResidualMax[1],
					new BigDecimal(20), new BigDecimal(50),null));
			prazoList.add(new Prazo(36, new BigDecimal("1.308033"), limiteResidualMin[2], limiteResidualMax[2],
					new BigDecimal(20), new BigDecimal(50),null));
		}

		Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
				new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
		Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
				new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);

		when(simulacaoServices.getTaxasIOF(any(), isNull())).then(i -> taxasIOF);
		when(simulacaoServices.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> prazoList);

		List<Seguro> seguros = Arrays.asList(spf, svp);
		when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
				.thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));
		return prazoList;
	}

	private SimulacaoParcResidualRequest criarRequest(Integer[] parcelas, double valorDoBem, Modalidade modalidade) {
		BigDecimal valor_uf_emplacamento = new BigDecimal(250.00);
		BigDecimal valor_cesta_servicos = new BigDecimal(200.00);
		BigDecimal valor_taxa_cadastro = new BigDecimal(130.00);
		String retorno = "1";
		Integer plano_id = 2752;
		String tipoPessoa = "pessoa-fisica";
		BigDecimal valor_bem = new BigDecimal(valorDoBem);
		BigDecimal valor_entrada = new BigDecimal(62910.00);
		BigDecimal valor_parcela_residual = new BigDecimal(31455.00);
		LocalDate dataPrimeiroVencimento = LocalDate.of(2019, 5, 23).plusDays(30);
		BigDecimal valor_seguro_auto = new BigDecimal(0);
		SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
		request.setModalidade(modalidade);
		request.setValorUfEmplacamento(valor_uf_emplacamento);
		request.setValorCestaServicos(valor_cesta_servicos);
		request.setValorTC(valor_taxa_cadastro);
	 	request.setVlrSeguroFranquia(new BigDecimal(1200));
		request.setRetorno(retorno);
		request.setPlanoId(plano_id);
		request.setTipoPessoa(tipoPessoa);
		request.setValorBem(valor_bem);
		request.setValorEntrada(valor_entrada);
		request.setValorParcelaResidual(valor_parcela_residual);
		request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
		request.setSeguroAuto(valor_seguro_auto);
		request.setCarenciaDoPlano(new Carencia(10, 180));
		request.setParcelas(parcelas);

		ParametrosDeSeguroPrestamista parametroDeSeguro = new ParametrosDeSeguroPrestamista(false, false, true, true,
				true, null, null, true, true, "", Arrays.asList(),null);
		request.setParametrosDeSeguroPrestamista(parametroDeSeguro);

		return request;
	}

	private SimulacaoParcResidualRequest criarRequest(Integer[] parcelas, double valorDoBem, Modalidade modalidade,
			Boolean permiteBalao, EnumControleBalao controleBalao) {
		SimulacaoParcResidualRequest request = this.criarRequest(parcelas, valorDoBem, modalidade);
		request.setControleBalao(controleBalao);
		request.setPermiteBalao(permiteBalao);
		return request;
	}

	private ItemFinanciavel getItemFinanciavel(int codigo, BigDecimal valor) {
		ItemFinanciavel i = new ItemFinanciavel();
		i.setCodigo(codigo);
		i.setValor(valor);
		return i;
	}



	@Test
	public void testarSimulacaoInternaPJ() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		mockSubsidioVariavel();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CICLO_TOYOTA,true, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
		request.setParametrosDeSeguroPrestamista(null);
		request.setTipoPessoa(TipoPessoa.PESSOA_JURIDICA.getValue().toLowerCase());
		request.setOrigemNegocio(new OrigemNegocio());
		// passando CPF para garantir que o serviço de parâmetros não será chamado
		// exatamente por se tratar de pessoa jurídica
		// e não porque o CPF não foi passado, o que daria o mesmo resultado
		request.setCpfProponente("CPF");
		// não pode ser maior que a maior taxa banco - 0.01
		request.setTaxaSubsidio(new BigDecimal("1.308033").subtract(new BigDecimal("0.01")));

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual("token", request);
		SimulacaoParcResidualResponse body = response.getBody();
		assertFalse(body.getPrazos().isEmpty());
		verifyNoMoreInteractions(params);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = P
	 */
	@Test
	public void testarSimulacaoInterna() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();
		mockSubsidioVariavel();

		Integer[] parcelas = { 12, 24, 36 };
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 104850.00, Modalidade.CICLO_TOYOTA, true,EnumControleBalao.PERMITE_BALAO);
		request.setOrigemNegocio(new OrigemNegocio());
		request.setCpfProponente("cpf");
		// não pode ser maior que a maior taxa banco - 0.01
		request.setTaxaSubsidio(new BigDecimal("1.308033").subtract(new BigDecimal("0.01")));

		ParametrosDeSeguroPrestamista p = new ParametrosDeSeguroPrestamista();
		when(params.getParametros(any(), anyString(), anyString(), any(), isNull())).thenReturn(p);
		controller.getParcelaResidual("token", request);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = O
	 */
	@Test(expected = BusinessValidationException.class)
	public void deveGerarErroQuandoPlanoForObrigatorioParcelasENaoPassarParcelas() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.OBRIGATORIO_BALAO);
		request.setValorParcelaResidual(null);
		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = NP
	 */
	@Test(expected = BusinessValidationException.class)
	public void deveGerarErroQuandoPlanoForNaoPermiteEHouverPassadoBalao() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.NAO_PERMITE_BALAO);
		request.setValorParcelaResidual(null);
		request.setIntermediarias(new ArrayList<>());
		request.getIntermediarias().add(new ParcelaIntermediaria());
		request.getIntermediarias().get(0).setValor(new BigDecimal(10));
		request.getIntermediarias().get(0).setVencimento(LocalDate.now());
		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = OU
	 */
	@Test(expected=BusinessValidationException.class)
	public void deveGerarErroQuandoPlanoForObrigatorioUltimaENaoHouverPassadoBalaoNaUltima() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 12, 24, 36 };
		// estamos usando mock para o simulacaoServices, então precisamos garantir que
		// os números de prazo são consistentes
		assertEquals(prazoList.size(), parcelas.length);
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
		request.setValorParcelaResidual(null);
		request.setIntermediarias(new ArrayList<>());
		request.getIntermediarias().add(new ParcelaIntermediaria());
		request.getIntermediarias().get(0).setValor(new BigDecimal(10));
		LocalDate vencimento = LocalDate.of(2019, 6, 1);
		request.getIntermediarias().get(0).setVencimento(vencimento);
		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = OU
	 */
	@Test(expected=BusinessValidationException.class)
	public void deveGerarErroQuandoPlanoForObrigatorioUltimaENaoHouverNenhumBalao() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 36 };
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
		request.setValorParcelaResidual(null);
		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);
		controller.getParcelaResidual(request);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = O
	 */
	@Test(expected=BusinessValidationException.class)
	public void deveGerarErroQuandoPlanoForObrigatorioBalaoENaoHouverNenhumBalao() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = { 36 };
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.OBRIGATORIO_BALAO);
		request.setValorParcelaResidual(null);
		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);
		controller.getParcelaResidual(request);
	}

	@Test
	public void deveGerarSimulacaoQuandoPlanoForObrigatorioBalaoEHouverAlgumBalao() {
		prepararMockDeSimulacaoServices();
		Integer[] parcelas = { 36 };
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.OBRIGATORIO_BALAO);
		request.setValorParcelaResidual(null);
		request.setIntermediarias(new ArrayList<>());
		request.getIntermediarias().add(new ParcelaIntermediaria());
		request.getIntermediarias().get(0).setValor(new BigDecimal(10));
		LocalDate vencimento = LocalDate.of(2019, 6, 1);
		request.getIntermediarias().get(0).setVencimento(vencimento);

		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);
		ResponseEntity<SimulacaoParcResidualResponse> responseEntity = controller.getParcelaResidual(request);
		assertEquals(200, responseEntity.getStatusCodeValue());
	}

	@Test
	public void deveGerarSimulacaoQuandoPlanoForOpcionalBalaoEHouverParcelaBalao() {
		prepararMockDeSimulacaoServices();
		Integer[] parcelas = { 36 };
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CDC, true,
				EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);
		request.setIntermediarias(new ArrayList<>());
		request.getIntermediarias().add(new ParcelaIntermediaria());
		request.getIntermediarias().get(0).setValor(new BigDecimal(10));
		LocalDate vencimento = LocalDate.of(2019, 6, 1);
		request.getIntermediarias().get(0).setVencimento(vencimento);

		List<ItemFinanciavel> itens = new ArrayList<>();

		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);
		ResponseEntity<SimulacaoParcResidualResponse> responseEntity = controller.getParcelaResidual(request);
		assertEquals(200, responseEntity.getStatusCodeValue());
	}

	/*
	 * Testa o caso CONTROLE BALÃO = OU e Ciclo
	 */
	@Test(expected = BusinessValidationException.class)
	public void deveGerarErroQuandoForCicloObrigatorioUltimaENaoHouverPassadoBalaoNaUltima() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices();

		Integer[] parcelas = {36};
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CICLO_TOYOTA, true,
				EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
		request.setValorParcelaResidual(null);

		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
		SimulacaoParcResidualResponse simulacao = response.getBody();
		List<PrazoResidualResponse> list = simulacao.getPrazos();

		list.forEach(r -> {
			assertEquals(TipoDeSeguroPrestamista.NENHUM, r.getTipoDeSeguroPrestamista());
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamista()) == 0);
			assertTrue("seguro não deve ser calculado",
					BigDecimal.ZERO.compareTo(r.getValorDoSeguroPrestamistaNaParcela()) == 0);
		});

		verify(simulacaoServices).getTaxasIOF(any(), isNull());
		verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
		verify(prestamistaService, new Times(1)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
		verify(simulacaoServices).getSubsidio(anyInt());
		verifyNoMoreInteractions(simulacaoServices);
	}

	/*
	 * Testa o caso CONTROLE BALÃO = O e Ciclo
	 */
	@Test(expected = BusinessValidationException.class)
	public void deveGerarErroQuandoForCicloComBalaoObrigatorio() {
		Integer[] parcelas = {36};
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 333500, Modalidade.CICLO_TOYOTA, true,
		EnumControleBalao.OBRIGATORIO_BALAO);
		request.setValorParcelaResidual(null);

		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(44000));
		itens.add(item);

		request.setItens(itens);

		controller.getParcelaResidual(request);
	}

	@Test
	@Ignore
	public void quandoRestaUmChamarPorPrazoNMesesCDCEntaoRetornaOpcaoCicloToyotaEquivalente() {
		//region Objeto SimulacaoParcResidualRequest
		SimulacaoParcResidualRequest simulacaoParcResidualRequest = new SimulacaoParcResidualRequest();
		simulacaoParcResidualRequest.setPeriodicidade(1);
		simulacaoParcResidualRequest.setCarenciaDoPlano(new Carencia(10, 190));
		simulacaoParcResidualRequest.setValorUfEmplacamento(new BigDecimal("364.59"));
		simulacaoParcResidualRequest.setValorBem(new BigDecimal("100000"));
		simulacaoParcResidualRequest.setValorEntrada(new BigDecimal("60000"));
		simulacaoParcResidualRequest.setValorCestaServicos(new BigDecimal("550.00"));
		simulacaoParcResidualRequest.setValorTC(new BigDecimal("550.00"));
		simulacaoParcResidualRequest.setRetorno("1");
		simulacaoParcResidualRequest.setPlanoId(4345);
		simulacaoParcResidualRequest.setTipoPessoa("pessoa-fisica");
		simulacaoParcResidualRequest.setIsentaIOF("N");
		simulacaoParcResidualRequest.setVlrSeguroFranquia(new BigDecimal(1200));
		simulacaoParcResidualRequest.setDataPrimeiroVencimento(LocalDate.of(2022,10,30));
		simulacaoParcResidualRequest.setValorParcelaResidual(new BigDecimal("40000"));
		simulacaoParcResidualRequest.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista());
		simulacaoParcResidualRequest.setParcelas(new Integer[]{55});
		//endregion

		//region Objeto TaxasIOF
		TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
		Plano plano = new Plano();
		plano.setCodigoModalidade("ciclo-toyota");
		plano.setPeriodicidadeNumero(1);
		plano.setPermiteBalao(true);
		plano.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
		//endregion

		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2022, Month.SEPTEMBER, 9, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2022, 9, 9))));

		//Rubens
		if(featureToggleConfig.getBuscaApiMotorTaxaEnabled()) {
			MotorTaxaPlanoService motorTaxaPlanoService = mock(MotorTaxaPlanoService.class);
			controller.setMotorTaxaPlanoService(motorTaxaPlanoService);
			when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(motorTaxaPlanoService.getPlanoPorId(anyInt()))).thenReturn(plano);
		} else {
			PlanoService planoService = mock(PlanoService.class);
			controller.setPlanoService(planoService);
			when(planoService.getPlanoById(anyInt())).thenReturn(plano);
		}

		when(simulacaoServices.getTaxasIOF(TipoPessoa.PESSOA_FISICA, "N")).thenReturn(taxasIOF);
		when(prestamistaService.getSegurosPrestamista(any(),any(), any(), anyBoolean(), anyBoolean())).
				thenReturn(mockSeguroPrestamistPlus()).thenReturn(mockSeguroPrestamistPlus());

		SimulacaoParcResidualResponse parcResidualResponse = controller.getParcelaResidualImpl(simulacaoParcResidualRequest, carregarPrazos());
		assertEquals(new BigDecimal("40000.00"),parcResidualResponse.getPrazos().get(0).getValorParcelaResidual());
		assertEquals(new BigDecimal("776.57"),parcResidualResponse.getPrazos().get(0).getValorParcela());
	}

	private List<Prazo> carregarPrazos() {
		return Arrays.asList(new Prazo(48, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null)
		);
	}

	private SeguroPrestamistaPlusResponse mockSeguroPrestamistPlus(){
		SeguroPrestamistaPlusResponse seguroPrestamista = new SeguroPrestamistaPlusResponse();
		Seguro seguro = new Seguro();
		seguro.setTipo(TipoDeSeguroPrestamista.SVP);
		seguro.setMaximoDaParcela(new BigDecimal("0.0"));
		seguro.setMaximoDoContrato(new BigDecimal("330000.0"));
		seguro.setMaximoPorCliente(new BigDecimal("990000.0"));
		seguro.setPercentualDoSeguro(new BigDecimal("0.016"));
		seguro.setIdadeMinima(18);
		seguro.setIdadeMaxima(65);
		seguro.setDescricao("SEGURO VIDA PRESTAMISTA");
		seguro.setFatorDesemprego(0.0);
		seguro.setFatorInvalidez(5.0);
		seguro.setFatorMorte(0.0155);
		seguro.setFormaCalculoDescricao("Sobre Valor Financiado");
		seguro.setFormaCalculoValor("VLR_FINANCIADO");
		seguro.setInicioVigencia(LocalDate.of(2022,07,01));
		seguro.setItemId(3);
		seguro.setOrdenacao(2);
		seguro.setPercentualIofSpf(0.38);
		seguro.setPercentualProLabore(30.0);
		seguro.setPrazoFinal(60);
		seguro.setPrazoInicio(1);
		seguro.setTempoMaximoContrato(60);

		seguroPrestamista.setUltimoSeguro(seguro);
		seguroPrestamista.setSeguros(new ArrayList<>(Arrays.asList(seguro)));

		return seguroPrestamista;
	}

	@Test
	public void quandoSimulacaoCom1PrazoEIntermediariasEntaoValidaValoresDasIntermediariasComSucesso() {
		Integer[] parcelas = { 36 };
		List<Prazo> prazoList = new ArrayList<>();
		prazoList.add(new Prazo(36, new BigDecimal("1.308033"), new BigDecimal("20.00"), new BigDecimal("40.00"), new BigDecimal(20), new BigDecimal(50),null));

		prepararMockDeSimulacaoServices();
		LocalDate dataCalculo = dataCarencia.getDataCalculo();

		SimulacaoParcResidualRequest request = criarRequest(parcelas, 300000, Modalidade.CDC, true,
				EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);

		ParcelaIntermediaria intermediaria1 = new ParcelaIntermediaria();
		intermediaria1.setValor(new BigDecimal("35000"));
		intermediaria1.setVencimento(request.getDataPrimeiroVencimento().plusMonths(2));

		ParcelaIntermediaria intermediaria2 = new ParcelaIntermediaria();
		intermediaria2.setValor(new BigDecimal("13000"));
		intermediaria2.setVencimento(request.getDataPrimeiroVencimento().plusMonths(10));
		request.setIntermediarias(Arrays.asList(intermediaria1, intermediaria2));

		controller.fazerSimulacao(request, dataCalculo, prazoList, null);
	}

	@Test(expected = BusinessValidationException.class)
	public void quandoSimulacaoCom1PrazoEIntermediariasAbaixoDoMinimoDoPlanoEntaoValidantermediariasRetornandoErro() {
		Integer[] parcelas = { 24 };
		List<Prazo> prazoList = new ArrayList<>();
		prazoList.add(new Prazo(24, new BigDecimal("1.308033"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));

		prepararMockDeSimulacaoServices();
		LocalDate dataCalculo = dataCarencia.getDataCalculo();

		SimulacaoParcResidualRequest request = criarRequest(parcelas, 200000, Modalidade.CDC, true,
				EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);

		ParcelaIntermediaria intermediaria1 = new ParcelaIntermediaria();
		intermediaria1.setValor(new BigDecimal("1500"));
		intermediaria1.setVencimento(request.getDataPrimeiroVencimento().plusMonths(2));

		ParcelaIntermediaria intermediaria2 = new ParcelaIntermediaria();
		intermediaria2.setValor(new BigDecimal("1000"));
		intermediaria2.setVencimento(request.getDataPrimeiroVencimento().plusMonths(10));
		request.setIntermediarias(Arrays.asList(intermediaria1, intermediaria2));

		request.setIntermediarias(Arrays.asList(intermediaria1, intermediaria2));

		controller.fazerSimulacao(request, dataCalculo, prazoList, null);
	}

	@Test(expected = BusinessValidationException.class)
	public void quandoSimulacaoCom1PrazoEIntermediariasAcimaDoMaximoDoPlanoEntaoValidantermediariasRetornandoErro() {
		Integer[] parcelas = { 48 };
		List<Prazo> prazoList = new ArrayList<>();
		prazoList.add(new Prazo(48, new BigDecimal("1.308033"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));

		prepararMockDeSimulacaoServices();
		LocalDate dataCalculo = dataCarencia.getDataCalculo();

		SimulacaoParcResidualRequest request = criarRequest(parcelas, 150000, Modalidade.CDC, true,
				EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);

		ParcelaIntermediaria intermediaria1 = new ParcelaIntermediaria();
		intermediaria1.setValor(new BigDecimal("65000"));
		intermediaria1.setVencimento(request.getDataPrimeiroVencimento().plusMonths(2));

		ParcelaIntermediaria intermediaria2 = new ParcelaIntermediaria();
		intermediaria2.setValor(new BigDecimal("10000"));
		intermediaria2.setVencimento(request.getDataPrimeiroVencimento().plusMonths(10));
		request.setIntermediarias(Arrays.asList(intermediaria1, intermediaria2));

		controller.fazerSimulacao(request, dataCalculo, prazoList, null);
	}
}
