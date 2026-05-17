package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.ResultadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.RestricaoPercentualBalao;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroMecanicaServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.DIRECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ActiveProfiles("test")
public class SimuladorParcResidualControllerCicloComIntermediariasTest{

	private SimuladorParcResidualController controller;
	private CalculadoraServices calculadoraServices = new CalculadoraServicesImpl();
	private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices = new CalculadoraSeguroMecanicaServicesImpl();

	@Mock
	private DataBARepository repository;

	@Mock
	private RedisRepository redisRepository;

	private DataCarencia dataCarencia;

	@Mock
	private SimulacaoServices simulacaoServices;

	@Mock
	private ParametrosDeSeguroPrestamistaService params;

	@Mock
	private LogService logService;

	@Mock
	private SeguroPrestamistaPlusServiceImpl prestamistaService;

	@Mock
	private FeatureToggleConfig featureToggleConfig;

	private LocalDate dataPrimeiroPagamento = LocalDate.now();

	@Mock
	private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

	@Before
	public void setUp() {

		MockitoAnnotations.initMocks(this);

		ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);

		dataCarencia = new DataCarenciaImpl(repository, redisRepository);
		when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(dataPrimeiroPagamento.minusDays(1))));
		controller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
				BigDecimal.TEN, params, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, new FatorSimulacao(), calculadoraSeguroMecanicaServices);

		when(featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(true);

		controller.setLogService(logService);

	}

	@Test
	public void simulacaoIntermediariasValidas() {
		List<Prazo> prazoList = prepararMockDeSimulacaoServices(true, true);
		when(simulacaoServices.getSubsidio(anyInt())).thenReturn(null);
		when(featureToggleConfig.getBuscaApiMotorTaxaEnabled() && featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(true);
		Integer prazo = 36;
		Integer[] parcelas = { prazo };
		assertEquals(1, prazoList.size());
		SimulacaoParcResidualRequest request = criarRequest(parcelas, 100000, Modalidade.CICLO_TOYOTA, EnumControleBalao.PERMITE_BALAO);
		request.setValorParcelaResidual(null);

		BigDecimal VINTE_MIL = new BigDecimal(20000);
		BigDecimal DEZ_MIL = new BigDecimal(10000);

		List<ParcelaIntermediaria> intermediarias = new ArrayList<ParcelaIntermediaria>() {{
			add(new ParcelaIntermediaria(DEZ_MIL, 9, dataPrimeiroPagamento.plusMonths(8)));
			add(new ParcelaIntermediaria(VINTE_MIL, 36, dataPrimeiroPagamento.plusMonths(prazo - 1)));
		}};

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
 			for (int j = 0; j < r.getFluxo().size(); j++) {
				Fluxo f = r.getFluxo().get(j);
				if (j == 9) {
					assertTrue("Intermediária opcional deve ser " + DEZ_MIL,
							DEZ_MIL.compareTo(f.getValorIntermediaria()) == 0);
				} else if (j == 36) {
					assertTrue("Parcela residual deve ser " + VINTE_MIL,
							VINTE_MIL.compareTo(f.getValorIntermediaria()) == 0);
				}
			}
		}
	}

	private List<Prazo> prepararMockDeSimulacaoServices(boolean soUmaParcela, boolean cdc) {
		TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");

		BigDecimal limiteResidualMin[] = new BigDecimal[3];
		BigDecimal limiteResidualMax[] = new BigDecimal[3];

			limiteResidualMin[0] = new BigDecimal("20.00");
			limiteResidualMax[0] = new BigDecimal("50.00");
			limiteResidualMin[1] = new BigDecimal("20.00");
			limiteResidualMax[1] = new BigDecimal("50.00");
			limiteResidualMin[2] = new BigDecimal("20.00");
			limiteResidualMax[2] = new BigDecimal("40.00");

		List<Prazo> prazoList = new ArrayList<>();
		BigDecimal limiteDeFinanciamento = new BigDecimal(330000);
		prazoList.add(new Prazo(36, new BigDecimal("1.308033"), limiteResidualMin[0], limiteResidualMax[0],
				new BigDecimal(20), new BigDecimal(50),null));

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

	private SimulacaoParcResidualRequest criarRequest(Integer[] parcelas, double valorDoBem, Modalidade modalidade, EnumControleBalao controleBalao) {


		SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
		request.setPermiteBalao(true);
		request.setBalaoUltima(false);
		request.setModalidade(modalidade);
		request.setValorUfEmplacamento(new BigDecimal(250.00));
		request.setValorCestaServicos(new BigDecimal(200.00));
		request.setValorTC(new BigDecimal(130.00));
		request.setRetorno("1");
		request.setPlanoId(2752);
		request.setTipoPessoa("pessoa-fisica");
		request.setValorBem(new BigDecimal(valorDoBem));
		request.setValorEntrada(new BigDecimal(30000.00));
		request.setVlrSeguroFranquia(new BigDecimal(1200));
		request.setDataPrimeiroVencimento(dataPrimeiroPagamento);
		request.setSeguroAuto(BigDecimal.ZERO);
		request.setCarenciaDoPlano(new Carencia(10, 180));
		request.setParcelas(parcelas);
		request.setQuantidadeIntermediarias(4);
		request.setIntervaloResidual(null);
		request.setControleBalao(controleBalao);

		RestricaoPercentualBalao restricao = new RestricaoPercentualBalao();
		restricao.setPrazoInicial(1);
		restricao.setPrazoFinal(24);
		restricao.setMinimo(new BigDecimal(10));
		restricao.setMaximo(new BigDecimal(50));
		restricao.setQuantidadeIntermediariasRange(1);

		request.setOrigemSolicitacao(DIRECT.getValue());
		Carencia carencia = new Carencia(0,0);
		request.setCarenciaDoPlano(carencia);

		request.setRestricoesPercentuaisBalao(new ArrayList<RestricaoPercentualBalao>(){{
			add(restricao);
		}});

		ParametrosDeSeguroPrestamista parametroDeSeguro = new ParametrosDeSeguroPrestamista(false, false, true, true,
				true, null, null, true, true, "", Arrays.asList(),null);
		request.setParametrosDeSeguroPrestamista(parametroDeSeguro);

		return request;
	}


}
