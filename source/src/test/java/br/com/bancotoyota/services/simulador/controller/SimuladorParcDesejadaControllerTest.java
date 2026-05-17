package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.beans.response.PrazoDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.SimulacaoServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SimuladorParcDesejadaControllerTest {

	private SimuladorParcDesejadaController controller;
	private SimuladorParcResidualController rcontroller;
	private SimuladorParcDesejadaNovaController ncontroller;

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
	private ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamista;

	@MockBean
	private LogService logService;

	@MockBean
	private SeguroPrestamistaPlusServiceImpl prestamistaService;

	private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

	@MockBean
	private FatorSimulacao fatorSimulacao;

	@MockBean
	private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

	private static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.findAndRegisterModules();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);

		dataCarencia = new DataCarenciaImpl(repository, redisRepository);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
		controller = new SimuladorParcDesejadaController(calculadoraServices, simulacaoServices, dataCarencia,
				parametrosDeSeguroPrestamista, false, prestamistaService,seguroFranquiaServices, calculadoraSeguroMecanicaServices);
		rcontroller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, parametrosDeSeguroPrestamista, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
		controller.getService().setLogService(logService);

		ncontroller = new SimuladorParcDesejadaNovaController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, parametrosDeSeguroPrestamista, false, prestamistaService,seguroFranquiaServices, calculadoraSeguroMecanicaServices);
		ncontroller.setMapper(mapper);
		ncontroller.setResidualService(new ResidualService(rcontroller));

		when(simulacaoServices.getParcelas(any(), any(), any(), isNull(), isNull())).then(i -> {
			return dadosDoPlano((Collection<Integer>) i.getArgument(2));
		});

		BigDecimal limiteDeFinanciamento = new BigDecimal(330000);
		Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
				new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
		spf.setOrdenacao(1);
		spf.setItemId(1);
		Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
				new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);
		svp.setOrdenacao(2);
		svp.setItemId(2);

		List<Seguro> seguros = Arrays.asList(spf, svp);
		when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
				.thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));

		TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
		when(simulacaoServices.getTaxasIOF(any(), isNull())).then(i -> taxasIOF);
	}

	@Test(expected=ServicoForaException.class)
	public void testeComErroClientesV2ServicoFora() throws Exception {
		when(redisRepository.getDataBA()).thenThrow(new ServicoForaException("Erro clientes v2",null));
		System.out.println("Locale: " + Locale.getDefault());
		System.out.println(new BigDecimal("1.2"));

		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1700));
		request.setValorUfEmplacamento(BigDecimal.ZERO);
		request.setValorCestaServicos(BigDecimal.ZERO);
		request.setValorTC(BigDecimal.ZERO);
		request.setRetorno("0");
		request.setValorBem(new BigDecimal(98000));
		request.setValorEntrada(new BigDecimal(48000));
		request.getParametrosDeSeguroPrestamista().setSvpDisponivel(true);
		request.getParametrosDeSeguroPrestamista().setSvpSeSpfRemovido(false);
		request.setCpfProponente("");

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);
		});
	}

	@Test(expected=ThirdPartyException.class)
	public void testeComErroClientesV2() throws Exception {
		when(redisRepository.getDataBA()).thenThrow(new ThirdPartyException("Erro clientes v2",null));
		System.out.println("Locale: " + Locale.getDefault());
		System.out.println(new BigDecimal("1.2"));

		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1700));
		request.setValorUfEmplacamento(BigDecimal.ZERO);
		request.setValorCestaServicos(BigDecimal.ZERO);
		request.setValorTC(BigDecimal.ZERO);
		request.setRetorno("0");
		request.setValorBem(new BigDecimal(98000));
		request.setValorEntrada(new BigDecimal(48000));
		request.getParametrosDeSeguroPrestamista().setSvpDisponivel(true);
		request.getParametrosDeSeguroPrestamista().setSvpSeSpfRemovido(false);
		request.setCpfProponente("");

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);
		});
	}

	/**
	 * Esse foi um teste executado manualmente pelo Luis e que havia dado problema
	 * com o valor da parcela no seguro.
	 */
	@Test
	public void teste2() throws Exception {

		System.out.println("Locale: " + Locale.getDefault());
		System.out.println(new BigDecimal("1.2"));

		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2300));
		request.setValorUfEmplacamento(BigDecimal.ZERO);
		request.setValorCestaServicos(BigDecimal.ZERO);
		request.setValorTC(BigDecimal.ZERO);
		request.setRetorno("0");
		request.setValorBem(new BigDecimal(98000));
		request.setValorEntrada(new BigDecimal(48000));
		request.getParametrosDeSeguroPrestamista().setSvpDisponivel(true);
		request.getParametrosDeSeguroPrestamista().setSvpSeSpfRemovido(false);
		request.setCpfProponente("");

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);
		});
	}

	@Test
	public void testeDesejada2300SemSeguroAuto() {
		testeDesejada2300Impl(null, true);
	}

	@Test
	public void testeDesejada2300ComSeguroAuto() {
		testeDesejada2300Impl(new BigDecimal(1000), true);
	}

	@Test
	public void testeDesejada2300ComSeguroAutoZero() {
		testeDesejada2300Impl(BigDecimal.ZERO, true);
	}

	@Test
	public void testeDesejada2300ComSeguroAutoEPlanoNaoPermiteBalao() {
		testeDesejada2300Impl(new BigDecimal(1000), false);
	}

	private void testeDesejada2300Impl(BigDecimal seguroAuto, Boolean permiteBalao) {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2300));
		request.setPermiteBalao(permiteBalao);
		request.setSeguroAuto(seguroAuto);

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			if (permiteBalao) {
				BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
						request.getValorBem());
				BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
						request.getValorBem());
				// quando o valor do residual chega no mínimo não é mais possível ficar tão
				// perto do valor desejado
				if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
						&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
					BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
					assertTrue(diferenca.compareTo(ResidualService.PRECISAO) <= 0);
				}
				printAndCheck(p);
			}

			if (p.getValorDoSeguroAutoNaParcela().compareTo(BigDecimal.ZERO) > 0) {
				if (!p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.NENHUM)) {
					BigDecimal fator = p.getValorDoSeguroAutoNaParcela().divide(seguroAuto, 2, RoundingMode.HALF_UP);
					BigDecimal fator2 = p.getValorDoSeguroPrestamistaNaParcela().divide(p.getValorDoSeguroPrestamista(),
							2, RoundingMode.HALF_UP);
					assertTrue("fatores de seguro devem ser iguais " + fator + " - " + fator2,
							fator.compareTo(fator2) == 0);
				}
			}
		});
	}

	private void printAndCheck(PrazoDesejadaResponse p) {
		if (false) {
			System.out.println("\n------------------------------");
			System.out.println("Valor financiado            : " + p.getValorTotalFinanciado());
			System.out.println("Parcela residual            : " + p.getValorParcelaResidual());
			System.out.println("Numero de Prazo             : " + p.getParcelas());
			System.out.println("Valor Parcela               : " + p.getValorParcela());
			System.out.println("Tipo de Seguro              : " + p.getTipoDeSeguroPrestamista());
			System.out.println("Valor do Seguro             : " + p.getValorDoSeguroPrestamista());
			System.out.println("Valor do Seguro Por Parcela : " + p.getValorDoSeguroPrestamistaNaParcela());
			System.out.println("------------------------------");
		}
		if (!p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.NENHUM)) {
			assertTrue("valor do seguro na parcela deve ser menor que a parcela",
					p.getValorDoSeguroPrestamistaNaParcela().compareTo(p.getValorParcela()) < 0);
			assertTrue(
					"soma das parcelas do seguro deve ser maior que o valor do seguro "
							+ p.getValorDoSeguroPrestamistaNaParcela().multiply(new BigDecimal(p.getParcelas())) + " - "
							+ p.getValorDoSeguroPrestamista(),
					p.getValorDoSeguroPrestamistaNaParcela().multiply(new BigDecimal(p.getParcelas()))
							.compareTo(p.getValorDoSeguroPrestamista()) > 0);
			assertTrue("valor do seguro na parcela deve ser menor que o valor total do seguro",
					p.getValorDoSeguroPrestamistaNaParcela().compareTo(p.getValorDoSeguroPrestamista()) < 0);
		}
	}

	@Test
	public void testeDoSeguro() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1500));
		request.setValorBem(new BigDecimal(80000));

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		PrazoDesejadaResponse p = r.getPrazos().get(0);
		BigDecimal valorDoSeguroSemItens = p.getValorDoSeguroPrestamistaNaParcela();
		TipoDeSeguroPrestamista tipoSeguroSemItens = p.getTipoDeSeguroPrestamista();

		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel i = new ItemFinanciavel();
		i.setCodigo(0);
		i.setValor(new BigDecimal(1000));
		itens.add(i);
		request.setItens(itens);

		response = ncontroller.getParcelaDesejada(request);
		r = response.getBody();
		p = r.getPrazos().get(0);
		BigDecimal valorDoSeguroComItens = p.getValorDoSeguroPrestamistaNaParcela();
		TipoDeSeguroPrestamista tipoSeguroComItens = p.getTipoDeSeguroPrestamista();
		assertEquals("tipo de seguro tem que ser o mesmo para o teste ser válido", tipoSeguroSemItens,
				tipoSeguroComItens);
		assertTrue("valor do seguro com item financiavel tem que ser maior que sem o item",
				valorDoSeguroComItens.compareTo(valorDoSeguroSemItens) > 0);
	}

	@Test
	public void testeDoSeguroGarantia() throws JsonProcessingException {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1500));
		request.setValorBem(new BigDecimal(80000));
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
		request.setParametrosSeguroMecanica(parametrosSeguroMecanica);
		CalculoSeguroMecanicaResponse calculoSeguroMecanicaResponse = new CalculoSeguroMecanicaResponse();
		SeguroMecanica seguroMecanica = new SeguroMecanica();
		seguroMecanica.setValorParcela(new BigDecimal(2000));
		seguroMecanica.setPrazo(12);
		calculoSeguroMecanicaResponse.setCotacoes(Collections.singletonList(seguroMecanica));
		when(calculadoraSeguroMecanicaServices.calcular(Mockito.any(), Mockito.any())).thenReturn(calculoSeguroMecanicaResponse);

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		PrazoDesejadaResponse p = r.getPrazos().get(0);


		response = ncontroller.getParcelaDesejada(request);
		r = response.getBody();
		p = r.getPrazos().get(0);
		assertEquals(1, p.getDadosSeguroMecanica().getCotacoes().size());

	}

	@Test
	public void testeDoPrestamistaComSeguroAuto() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1500));
		request.setSeguroAuto(BigDecimal.ZERO);
		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		PrazoDesejadaResponse p = r.getPrazos().get(0);
		BigDecimal valorDoPrestamistaSemSeguroAuto = p.getValorDoSeguroPrestamistaNaParcela();
		TipoDeSeguroPrestamista tipoPrestamistaSemSeguro = p.getTipoDeSeguroPrestamista();
		request.setSeguroAuto(new BigDecimal(3000));

		response = ncontroller.getParcelaDesejada(request);
		r = response.getBody();
		p = r.getPrazos().get(0);
		BigDecimal valorDoPrestamistaComSeguroAuto = p.getValorDoSeguroPrestamistaNaParcela();
		TipoDeSeguroPrestamista tipoPrestamistaComSeguro = p.getTipoDeSeguroPrestamista();
		if (tipoPrestamistaSemSeguro.equals(tipoPrestamistaComSeguro)) {
			assertTrue("valor do prestamista com seguro auto tem que ser maior que sem o seguro auto",
					valorDoPrestamistaComSeguroAuto.compareTo(valorDoPrestamistaSemSeguroAuto) > 0);
		} else {
			assertTrue("valor do prestamista com seguro auto tem que ser menor que sem o seguro auto",
					valorDoPrestamistaComSeguroAuto.compareTo(valorDoPrestamistaSemSeguroAuto) < 0);
		}
	}

	@Test
	public void testeDesejada2500() throws Exception {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));

		String json = mapper.writeValueAsString(request);
		SimulacaoParcResidualRequest rrequest = mapper.readValue(json, SimulacaoParcResidualRequest.class);

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(
						"diferença para o valor desejado (" + diferenca + ") deve ser menor que a precisão ("
								+ ResidualService.PRECISAO + ") para o prazo " + p.getParcelas(),
						diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);

			if (p.getParcelas() == 24) {
				// a simulação pela parcela residual precisa dar o mesmo resultado
				rrequest.setValorParcelaResidual(p.getValorParcelaResidual());
				rrequest.setParcelas(new Integer[] { p.getParcelas() });
				ResultadoParcelaResidual l = rcontroller.fazerSimulacao(rrequest, true, false,
						dataCarencia.getDataCalculo(), false);
				ResultadoCalculadoParcelaResidual res = l.getList().get(0);
				assertEquals(res.getValorParcela().setScale(2, RoundingMode.HALF_UP), p.getValorParcela());
				assertEquals(res.getDadosSeguroPrestamista().getTipo(), p.getTipoDeSeguroPrestamista());
				System.out.println(res.getValorParcela() + " - " + res.getDadosSeguroPrestamista().getTipo());

				System.out.println(p.getValorParcelaResidual());
				rrequest.setValorParcelaResidual(p.getValorParcelaResidual().add(new BigDecimal("1569.7")));
				l = rcontroller.fazerSimulacao(rrequest, dataCarencia.getDataCalculo(), null,
						/* TipoDeSeguroPrestamista.SPF */null);
				res = l.getList().get(0);
				System.out.println(res.getValorParcela() + " - " + res.getDadosSeguroPrestamista().getTipo());
				System.out.println(rrequest.getValorParcelaResidual());
			}

			if (p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.SPF)) {
				assertFalse("SPF não aceita parcela maior que 2500",
						p.getValorParcela().compareTo(new BigDecimal(2500)) > 0);
			}
		});
	}

	@Test
	public void testeDataCalculoVazia() throws Exception {
		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(null);
		when(redisRepository.getDataBA()).thenReturn(controleBA);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(null)));

		LocalDate dataCalculo = dataCarencia.getDataCalculo();
		LocalDate dataBAAlternativa = LocalDate.now();
		while (dataBAAlternativa.getDayOfWeek() == DayOfWeek.SATURDAY
				|| dataBAAlternativa.getDayOfWeek() == DayOfWeek.SUNDAY) {
			dataBAAlternativa = dataBAAlternativa.plusDays(Constants.UM);
		}

		assertEquals(dataCalculo,dataBAAlternativa);
	}

	@Test
	public void testeDataCalculoMongoMaior() throws Exception {
		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(null);
		when(redisRepository.getDataBA()).thenReturn(controleBA);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));

		LocalDate dataCalculo = dataCarencia.getDataCalculo();
		assertEquals(dataCalculo,LocalDate.of(2019, 02, 18));
	}

	@Test
	public void testeDataCalculoRedisMaior() throws Exception {
		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, 02, 18,0,0,0));

		when(redisRepository.getDataBA()).thenReturn(controleBA);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2019, 02, 17))));

		LocalDate dataCalculo = dataCarencia.getDataCalculo();
		assertEquals(dataCalculo,LocalDate.of(2019, 02, 18));
	}




	@Test
	public void testeDesejada2600() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2600));
		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		boolean spf = false;
		boolean svp = false;
		for (PrazoDesejadaResponse p : r.getPrazos()) {
			System.out.println(p.getParcelas() + " - " + p.getValorParcela() + " - " + p.getTipoDeSeguroPrestamista()
					+ " - " + p.getValorParcelaResidual());
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// Quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado.
			// O mesmo vale para quando é SPF, pois o limite é de 2500.
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0
					&& p.getTipoDeSeguroPrestamista() != TipoDeSeguroPrestamista.SPF) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(
						"diferença para o valor desejado (" + diferenca + ") deve ser menor que a precisão ("
								+ ResidualService.PRECISAO + ") para o prazo " + p.getParcelas(),
						diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);

			if (p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.SPF)) {
				spf = true;
				assertFalse("SPF não aceita parcela maior que 2500 - " + p.getParcelas(),
						p.getValorParcela().compareTo(new BigDecimal(2500)) > 0);
			}
			if (p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.SVP)) {
				svp = true;
			}
		}
		assertTrue("SVP deve ter sido usado", svp);
		assertTrue("SPF deve ter sido usado", spf);
	}

	@Test(expected = BusinessValidationException.class)
	public void testeParcelaNegativa() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		// Com entrada de 84373.5 o valor da parcela em 12x é zero
		// acima desse valor começamos a ter parcelas negativas.
		// Como o percentual mínimo para o residual é 20%, para valores de entrada
		// maiores do que 80% o valor do residual
		// vai sempre esbarrar nesse limite mínimo.
		request.setValorEntrada(new BigDecimal("81210.69"));

		ncontroller.getParcelaDesejada(request);
	}

	@Test
	public void testePJ() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		request.setTipoPessoa(TipoPessoa.PESSOA_JURIDICA.getValue().toLowerCase());

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			printAndCheck(p);
			assertEquals(TipoDeSeguroPrestamista.NENHUM, p.getTipoDeSeguroPrestamista());
		});
	}

	@Test
	public void testePJSemIOF() throws IOException {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		request.setTipoPessoa(TipoPessoa.PESSOA_JURIDICA.getValue().toLowerCase());
		request.setIsentaIOF("S");

		RedisRepository redisRepository = mock(RedisRepository.class);

		MotorTaxaPlanoService motorTaxaPlanoService=null;
		PlanoService planoService=null;
		if(featureToggleConfig.getBuscaApiMotorTaxaEnabled()) {
			motorTaxaPlanoService = mock(MotorTaxaPlanoService.class);
			when(motorTaxaPlanoService.findPrazos(any(), any())).then(i -> dadosDoPlano(i.getArgument(1)));
		} else {
			planoService = mock(PlanoService.class);
			when(planoService.findPrazos(any(), any())).then(i -> dadosDoPlano(i.getArgument(1)));
		}
		// o novo simulador pela parcela desejada usa o residual internamente
		rcontroller.simulacaoServices = new SimulacaoServicesImpl(redisRepository, planoService, motorTaxaPlanoService, featureToggleConfig);

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();
		r.getPrazos().stream().forEach(p -> {
			assertTrue("valor do IOF deve ser zero pois foi passado que esta isento de IOF " + p.getValorIOF(),
					p.getValorIOF().compareTo(BigDecimal.ZERO) == 0);
		});

		r.getPrazos().forEach(p -> {
			printAndCheck(p);
			assertEquals(TipoDeSeguroPrestamista.NENHUM, p.getTipoDeSeguroPrestamista());
		});
	}

	@Test
	public void testeValorFinanciadoMuitoAlto() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		request.setValorBem(new BigDecimal(450000));

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			printAndCheck(p);
			assertEquals(TipoDeSeguroPrestamista.NENHUM, p.getTipoDeSeguroPrestamista());
		});
	}

	@Test
	public void testeValortotalFinanciadoEstourandoLimite() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		List<ItemFinanciavel> itens = new ArrayList<>();
		ItemFinanciavel item = new ItemFinanciavel();
		item.setCodigo(1);
		item.setDescricao("teste");
		item.setValor(new BigDecimal(24000));
		itens.add(item);

		request.setItens(itens);
		request.setValorBem(new BigDecimal(330000));

		ResponseEntity<SimulacaoParcDesejadaResponse> response = ncontroller.getParcelaDesejada(request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			printAndCheck(p);
			assertEquals(TipoDeSeguroPrestamista.NENHUM, p.getTipoDeSeguroPrestamista());
		});
	}

	@Test(expected = BusinessValidationException.class)
	public void testDataInicioInvalida_1() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		LocalDate dataPrimeiroVencimento = LocalDate.of(2019, 5, 23).plusDays(181);
		request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
		ncontroller.getParcelaDesejada(request);
	}

	@Test(expected = BusinessValidationException.class)
	public void testDataInicioInvalida_2() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		LocalDate dataPrimeiroVencimento = dataCarencia.getDataCalculo().plusDays(9);
		request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
		ncontroller.getParcelaDesejada(request);
	}

	@Test
	public void testSemTaxaDeSubsidio() {
		when(simulacaoServices.getSubsidio(anyInt()))
				.thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.VARIAVEL).build());
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		try {
			ncontroller.getParcelaDesejada(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("plano com subsídio requer valor-subsidio ou taxa-subsidio", ex.getMessage());
		}
	}

	@Test
	public void testSemRetorno() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		request.setRetorno(null);
		try {
			ncontroller.getParcelaDesejada(request);
			fail();
		} catch (BusinessValidationException ex) {
			assertEquals("Campo obrigatório para plano sem subsídio", ex.getMessage());
		}
	}

	@Test
	public void testeDoCalcularResiduais() throws Exception {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2600));
		request.setValorEntrada(new BigDecimal(50500));

		Map<Integer, Prazo> prazosMap = controller.getService().getPrazosMap(request.getPlanoId(),
				request.getPercentualEntrada(), null, null);
		BigDecimal valorBaseParaCalculoDoSeguro = request.calcularValorBaseParaCalculoDoSeguro();

		TaxasIOF taxasIOF = simulacaoServices.getTaxasIOF(request.getTipoPessoaEnum(), null);

		DadosCalculo dadosCalculo = new DadosCalculo(dataCarencia.getDataCalculo(), new IOF(taxasIOF),
				prestamistaService.getSegurosPrestamista("CNPJ", null, Arrays.asList(), false, false), valorBaseParaCalculoDoSeguro, null, null, null);
		Map<Integer, ResultadoCalculadoParcelaDesejada> map = controller.getService().calcularResiduais(request,
				prazosMap, dadosCalculo);

		map.forEach((key, r) -> {
			System.out.println(key + " - " + r.getValorParcela() + " - " + r.getDadosSeguroPrestamista().getTipo()
					+ " diferença: " + (request.getValorParcelaDesejada().subtract(r.getValorParcela())) + " residual: "
					+ r.getValorParcelaResidual());
			BigDecimal percentualParcelaResidual = r.getValorParcelaResidual().multiply(new BigDecimal(100))
					.divide(request.getValorBem(), 2, RoundingMode.HALF_UP);
			Prazo prazo = prazosMap.get(r.getPrazo());
			assertTrue(
					"porcentagem da parcela residual mínimo deve ser respeitado " + percentualParcelaResidual + " - "
							+ prazo.getPercentualMinBalao(),
					percentualParcelaResidual.compareTo(prazo.getPercentualMinBalao()) >= 0);
			assertTrue(
					"porcentagem da parcela residual máximo deve ser respeitado " + percentualParcelaResidual + " - "
							+ prazo.getPercentualMaxBalao(),
					percentualParcelaResidual.compareTo(prazo.getPercentualMaxBalao()) <= 0);
			if (r.getDadosSeguroPrestamista().getTipo() == TipoDeSeguroPrestamista.SPF) {
				assertTrue("SPF só pode ser usado com parcela menor que 2500, parcela atual: " + r.getValorParcela(),
						r.getValorParcela().compareTo(new BigDecimal(2500)) <= 0);
			}
		});
	}

	private BigDecimal getMinParcelaBalao(int prazo, int plano, BigDecimal valor) {
		Map<Integer, Prazo> map = controller.getService().getPrazosMap(plano, BigDecimal.valueOf(25.000000), null,
				null);
		Prazo p = map.get(prazo);
		return p.getPercentualMinBalao().multiply(valor).divide(new BigDecimal(100));
	}

	private BigDecimal getMaxParcelaBalao(int prazo, int plano, BigDecimal valor) {
		Map<Integer, Prazo> map = controller.getService().getPrazosMap(plano, BigDecimal.valueOf(25.000000), null,
				null);
		Prazo p = map.get(prazo);
		return p.getPercentualMaxBalao().multiply(valor).divide(new BigDecimal(100));
	}

	private SimulacaoParcDesejadaRequest criarRequest(BigDecimal valor_parcela_desejada) {
		BigDecimal valor_uf_emplacamento = new BigDecimal(250.00);
		BigDecimal valor_cesta_servicos = new BigDecimal(200.00);
		BigDecimal valor_taxa_cadastro = new BigDecimal(130.00);
		String retorno = "0";
		Integer plano_id = 2752;
		String tipoPessoa = "pessoa-fisica";
		BigDecimal valor_bem = new BigDecimal(100000);
		BigDecimal valor_seguro_franquia = new BigDecimal(1200);
		BigDecimal valor_entrada = new BigDecimal(40000);
		LocalDate dataPrimeiroVencimento = LocalDate.of(2019, 5, 23).plusDays(30);
		SimulacaoParcDesejadaRequest request = new SimulacaoParcDesejadaRequest();

		request.setValorUfEmplacamento(valor_uf_emplacamento);
		request.setValorCestaServicos(valor_cesta_servicos);
		request.setValorTC(valor_taxa_cadastro);
		request.setRetorno(retorno);
		request.setPlanoId(plano_id);
		request.setTipoPessoa(tipoPessoa);
		request.setValorBem(valor_bem);
		request.setVlrSeguroFranquia(valor_seguro_franquia);
		request.setValorEntrada(valor_entrada);
		request.setValorParcelaDesejada(valor_parcela_desejada);
		request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
		request.setCarenciaDoPlano(new Carencia(10, 180));
		ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false, true, true, true, null, null,true,true, "", Arrays.asList(),null);
		request.setParametrosDeSeguroPrestamista(parametrosDeSeguroPrestamista);
		System.out.println("\n-----  PARAMETROS ------------------------------");
		System.out.println("Valor do Bem              : " + valor_bem);
		System.out.println("Valor de Entrada          : " + valor_entrada);
		System.out.println("Valor da Parcela Desejada : " + valor_parcela_desejada);
		System.out.println("-----------------------------------");
		return request;
	}

	public static List<Prazo> dadosDoPlano(Collection<Integer> meses) throws IOException {
		String[] valores = new String[] {
				"{ \"prazo\": \"12\", \"taxa-mes\": \"1.480427\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"13\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"14\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"15\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"16\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"17\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"18\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"19\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"20\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"21\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"22\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"23\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"24\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"25\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"40.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"26\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"40.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"27\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"40.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }",
				"{ \"prazo\": \"28\", \"taxa-mes\": \"1.439864\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"40.00\", \"perc-min-entrada\": \"0\", \"perc-max-entrada\": \"100\" }" };
		List<Prazo> list = new ArrayList<>();
		for (String valor : valores) {
			list.add(mapper.readValue(valor, Prazo.class));
		}
		if (meses != null && !meses.isEmpty()) {
			return list.stream().filter(p -> meses.contains(p.getNumeroDeParcelas())).collect(Collectors.toList());
		}
		return list;
	}

	@Test
	public void testarSimulacaoInterna() {
		SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
		request.setOrigemNegocio(new OrigemNegocio());
		request.setCpfProponente("proponente");

		when(parametrosDeSeguroPrestamista.getParametros(any(), anyString(), anyString(), any(), isNull()))
				.thenReturn(request.getParametrosDeSeguroPrestamista());

		ResponseEntity<SimulacaoParcDesejadaResponse> response = controller.getParcelaDesejada("token", request);

		SimulacaoParcDesejadaResponse r = response.getBody();

		r.getPrazos().forEach(p -> {
			BigDecimal minParcelaBalao = getMinParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			BigDecimal maxParcelaBalao = getMaxParcelaBalao(p.getParcelas(), request.getPlanoId(),
					request.getValorBem());
			// quando o valor do residual chega no mínimo não é mais possível ficar tão
			// perto do valor desejado
			if (p.getValorParcelaResidual().compareTo(minParcelaBalao) > 0
					&& p.getValorParcelaResidual().compareTo(maxParcelaBalao) < 0) {
				BigDecimal diferenca = request.getValorParcelaDesejada().subtract(p.getValorParcela()).abs();
				assertTrue(
						"diferença para o valor desejado (" + diferenca + ") deve ser menor que a precisão ("
								+ ResidualService.PRECISAO + ") para o prazo " + p.getParcelas(),
						diferenca.compareTo(ResidualService.PRECISAO) <= 0);
			}
			printAndCheck(p);

			if (p.getTipoDeSeguroPrestamista().equals(TipoDeSeguroPrestamista.SPF)) {
				assertFalse("SPF não aceita parcela maior que 2500",
						p.getValorParcela().compareTo(new BigDecimal(2500)) > 0);
			}
		});
	}
}
