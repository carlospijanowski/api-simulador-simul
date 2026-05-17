package br.com.bancotoyota.services.simulador.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroFranquiaServicesImpl;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.unitils.reflectionassert.ReflectionAssert;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;



import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;

import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.HttpStatusInvalidException;

import br.com.bancotoyota.services.simulador.services.impl.RestaUmServiceImpl;

@RunWith(SpringRunner.class)
public class SimuladorSimplificadoControllerTest extends SimuladorSimplificadoControllerBaseTest  {

    SimuladorSimplificadoController simuladorSimplificadoController;
    SimuladorParcDesejadaController simuladorParcDesejadaController;
    SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController;
    SimuladorParcResidualController simuladorParcResidualController;
    RestaUmService restaUmService;
    @MockBean
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);


    @MockBean
    private FatorSimulacao fatorSimulacao;

    private CalculadoraSeguroFranquiaServices seguroFranquiaServices = new CalculadoraSeguroFranquiaServicesImpl();

    private static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.findAndRegisterModules();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

    @Before
    public void setup() throws HttpStatusInvalidException {
        super.setup();
        MockitoAnnotations.initMocks(this);


        simuladorParcDesejadaController = new SimuladorParcDesejadaController(calculadoraService, simulacaoService,
                dataCarencia, parametrosDeSeguroPrestamistaService, false, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
        simuladorParcResidualController = new SimuladorParcResidualController(calculadoraService, simulacaoService,
                dataCarencia, subsidioMinimoPorValor, parametrosDeSeguroPrestamistaService, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
        restaUmService = new RestaUmServiceImpl(emplacamentoService,
                origemNegocioService, cestaServicoService, planoService, motorTaxaPlanoService, clientService, simuladorParcResidualController, featureToggleConfig);
        ((RestaUmServiceImpl)restaUmService).setSimuladorParcDesejadaController(simuladorParcDesejadaController);

        simuladorParcDesejadaNovaController = new SimuladorParcDesejadaNovaController(calculadoraService, simulacaoService,
                dataCarencia, BigDecimal.TEN, parametrosDeSeguroPrestamistaService, false, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        simuladorParcDesejadaNovaController.setMapper(mapper);
        simuladorParcDesejadaNovaController.setResidualService(new ResidualService(simuladorParcResidualController));

        simuladorSimplificadoController = new SimuladorSimplificadoController(simuladorParcResidualController, restaUmService,veiculosService,simulacaoSimplificadaService,propostasFinanciamentoService);

        try {
			Field veiculosEnabled = simuladorSimplificadoController.getClass().getDeclaredField("veiculosEnabled");
			Boolean veiculosEn = true;
			veiculosEnabled.setAccessible(true);
			veiculosEnabled.set(simuladorSimplificadoController, veiculosEn);

			Field simulacaoSimplificadaParceirosEnabled = simuladorSimplificadoController.getClass().getDeclaredField("simulacaoSimplificadaParceirosEnabled");
			Boolean simulacaoSimplificadaParceirosEn = true;
			simulacaoSimplificadaParceirosEnabled.setAccessible(true);
			simulacaoSimplificadaParceirosEnabled.set(simuladorSimplificadoController, simulacaoSimplificadaParceirosEn);

			Field simulacaoSimplificadaParceirosEnabledRestaUm = restaUmService.getClass().getDeclaredField("simulacaoSimplificadaParceirosEnabled");
			Boolean simulacaoSimplificadaParceirosEnRestaUm = true;
			simulacaoSimplificadaParceirosEnabledRestaUm.setAccessible(true);
			simulacaoSimplificadaParceirosEnabledRestaUm.set(restaUmService, simulacaoSimplificadaParceirosEnRestaUm);

			Field filtroIdModalidadeEnabledSim = simuladorSimplificadoController.getClass().getDeclaredField("filtroIdModalidadeEnabled");
			Boolean filtroIdModalidadeEnSim = true;
			filtroIdModalidadeEnabledSim.setAccessible(true);
			filtroIdModalidadeEnabledSim.set(simuladorSimplificadoController, filtroIdModalidadeEnSim);

		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        simuladorSimplificadoController.setSimuladorParcDesejadaService(simuladorParcDesejadaController.getService());
        simuladorSimplificadoController.setSimuladorParcDesejadaNovaController(simuladorParcDesejadaNovaController);

        ReflectionTestUtils.setField(simuladorParcResidualController, "motorTaxaPlanoService", motorTaxaPlanoService);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaPrazoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setParcelas(new Integer[] {12});
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setParcelas(new Integer[] {12});
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCicloUsadoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CICLO_TOYOTA);
        simulacaoSimplParcDesejadaRequest.setSituacaoVeiculo(SituacaoVeiculo.USADO);
        simulacaoSimplParcDesejadaRequest.setAnoModelo(2020);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCicloNovoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCicloCodigoRetornoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setRetorno("1");
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        simulacaoSimplParcDesejadaRequest.setCodigoRetorno("1");
        simulacaoSimplParcDesejadaRequest.setSessionId("323133-35464-343uifadbe4d");
        simulacaoSimplParcDesejadaRequest.setOrigemSolicitacao("TDB");
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }



    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCdcRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, null);
        simulacaoParcResidualRequest.setPlanoId(3300);
        simulacaoParcResidualRequest.setModalidade(Modalidade.CDC);

            simuladorParcResidualController.setMotorTaxaPlanoService(motorTaxaPlanoService);


        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CDC);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);
        //apagando valores que a variável da residual não possui para validar somente as parcelas
        actual.getBody().setValorRegistroContrato(null);
        actual.getBody().setValorCestaServico(null);
        actual.getBody().setValorTarifaCadastro(null);


        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));

        assertEquals(expected.getBody().getPrazos().size(), actual.getBody().getPrazos().size());
        ReflectionAssert.assertReflectionEquals(expected.getBody().getPrazos(), actual.getBody().getPrazos());
    }

    private Plano getPlano() {
        return new Plano("CDC", PLANO_ID, "1", "LXT>40 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList(MARCA_ID)), new BigDecimal("40.000000"), new BigDecimal("100.000000"),
                new BigDecimal("0.00"), CINQUENTA_PERC, CARENCIA_MIN, CARENCIA_MAX, "CDC-TCM", false, null, false,
                "AI_E_0KM", 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.PERMITE_BALAO,null,null,null);

    }

    private PlanoMotorTaxa getMotorTaxaPlano() {

        ParcelasIntermediarias parcInt = new ParcelasIntermediarias();
        parcInt.setPermiteBalao(Boolean.TRUE);

        return PlanoMotorTaxa.builder()
                .planoId(Integer.parseInt(PLANO_ID))
                .descricao("")
                .id(null)
                .parcelasIntermediarias(parcInt)
                .build();

        /*return new MotorTaxaPlanoResponse("CDC", PLANO_ID, "1", "LXT>40 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList(MARCA_ID)), new BigDecimal("40.000000"), new BigDecimal("100.000000"),
                new BigDecimal("0.00"), CINQUENTA_PERC, CARENCIA_MIN, CARENCIA_MAX, "CDC-TCM", false, null, false,
                "AI_E_0KM", 30, "12", "N", "MENSAL", 1, true,EnumControleBalao.PERMITE_BALAO); */

    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCdcUsadoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, null);
        simulacaoParcResidualRequest.setPlanoId(3300);
        simulacaoParcResidualRequest.setModalidade(Modalidade.CDC);

        PlanoService planoService = mock(PlanoService.class);
        when(planoService.getPlanoById(anyInt()))
                .thenReturn(getPlano());
        simuladorParcResidualController.setPlanoService(planoService);
        simuladorParcResidualController.setMotorTaxaPlanoService(motorTaxaPlanoService);
        Plano plano = new Plano();
        plano.setPermiteBalao(true);
        when(planoService.getPlanoById(any())).thenReturn(getPlano());
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(any())).thenReturn(getPlano());

        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CDC);
        simulacaoSimplParcDesejadaRequest.setSituacaoVeiculo(SituacaoVeiculo.USADO);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);
        //apagando valores que a variável da residual não possui para validar somente as parcelas
        actual.getBody().setValorRegistroContrato(null);
        actual.getBody().setValorCestaServico(null);
        actual.getBody().setValorTarifaCadastro(null);


        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));

        assertEquals(expected.getBody().getPrazos().size(), actual.getBody().getPrazos().size());
        ReflectionAssert.assertReflectionEquals(expected.getBody().getPrazos(), actual.getBody().getPrazos());
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCdcNovoRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, null);
        simulacaoParcResidualRequest.setPlanoId(3300);
        simulacaoParcResidualRequest.setModalidade(Modalidade.CDC);

        PlanoService planoService = mock(PlanoService.class);
        when(planoService.getPlanoById(anyInt()))
                .thenReturn(getPlano());
        simuladorParcResidualController.setPlanoService(planoService);
        simuladorParcResidualController.setMotorTaxaPlanoService(motorTaxaPlanoService);
        Plano plano = new Plano();
        plano.setPermiteBalao(true);
        when(planoService.getPlanoById(any())).thenReturn(getPlano());
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(any())).thenReturn(getPlano());

        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CDC);
        simulacaoSimplParcDesejadaRequest.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);
        //apagando valores que a variável da residual não possui para validar somente as parcelas
        actual.getBody().setValorRegistroContrato(null);
        actual.getBody().setValorCestaServico(null);
        actual.getBody().setValorTarifaCadastro(null);


        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));

        assertEquals(expected.getBody().getPrazos().size(), actual.getBody().getPrazos().size());
        ReflectionAssert.assertReflectionEquals(expected.getBody().getPrazos(), actual.getBody().getPrazos());
    }


    @Test
    public void shouldBeEqualsSimulacaoParcResidualRequest() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual(simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualUsadoRequest() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoSimplificadaResidualRequest.setSituacaoVeiculo(SituacaoVeiculo.USADO);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual(simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualNovoRequest() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoSimplificadaResidualRequest.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual(simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualCodigoRetornoRequest() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoParcResidualRequest.setRetorno("1");
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoSimplificadaResidualRequest.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        simulacaoSimplificadaResidualRequest.setCodigoRetorno("1");
        simulacaoSimplificadaResidualRequest.setSessionId("323133-35464-343uifadbe4d");
        simulacaoSimplificadaResidualRequest.setOrigemSolicitacao("TDB");
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual(simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaClienteAtivo() throws Exception {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setValorTC(BigDecimal.ZERO);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada, CLIENTE_ATIVO_CPF);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada(simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualClienteInativo() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual, CLIENTE_INATIVO_CPF);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual(simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualComTaxaSubsidio() throws Exception {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);
        BigDecimal taxaSubsidio = new BigDecimal(1.29);
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL)
                .vpPercentualDistrMarca(new BigDecimal(100))
                .vpValorMaxMarca(new BigDecimal(9999999))
                .build();
        when(simulacaoService.getSubsidio(anyInt())).thenReturn(subsidio);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setTaxaSubsidio(taxaSubsidio);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setTaxaSubsidio(taxaSubsidio);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualComValorSubsidio() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);
        BigDecimal valorSubsidio = new BigDecimal(1234.56);
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL)
                .vpPercentualDistrMarca(new BigDecimal(100))
                .vpValorMaxMarca(new BigDecimal(9999999))
                .build();
        when(simulacaoService.getSubsidio(anyInt())).thenReturn(subsidio);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoParcResidualRequest.setValorSubsidio(valorSubsidio);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoSimplificadaResidualRequest.setValorSubsidio(valorSubsidio);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual( simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualComSeguro() throws Exception {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);
        BigDecimal valorSeguro = new BigDecimal(3600);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setSeguroAuto(valorSeguro);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
        		simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setSeguroAuto(valorSeguro);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualComSeguroGarantia() throws Exception {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);
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
        CalculoSeguroMecanicaResponse calculoSeguroMecanicaResponse = new CalculoSeguroMecanicaResponse();
        SeguroMecanica seguroMecanica = new SeguroMecanica();
        seguroMecanica.setValorParcela(new BigDecimal(2000));
        seguroMecanica.setPrazo(12);
        calculoSeguroMecanicaResponse.setCotacoes(Collections.singletonList(seguroMecanica));
        when(calculadoraSeguroMecanicaServices.calcular(Mockito.any(), Mockito.any())).thenReturn(calculoSeguroMecanicaResponse);

        SimulacaoParcDesejadaRequest simulacaoParcDesejadaRequest =
                criarSimulacaoParcDesejadaRequest(valorParcelaDesejada);
        simulacaoParcDesejadaRequest.setParametrosSeguroMecanica(parametrosSeguroMecanica);
        ResponseEntity<SimulacaoParcDesejadaResponse> expected =
                simuladorParcDesejadaNovaController.getParcelaDesejada(simulacaoParcDesejadaRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setParametrosSeguroMecanica(parametrosSeguroMecanica);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcResidualComItens() throws Exception {
        BigDecimal valorParcelaResidual = BigDecimal.valueOf(10000);
        List<ItemFinanciavel> itens = new ArrayList<>();
        itens.add(getItemFinanciavel(1, new BigDecimal(1000)));
        itens.add(getItemFinanciavel(2, new BigDecimal(2000)));

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoParcResidualRequest.setItens(itens);
        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaResidualRequest simulacaoSimplificadaResidualRequest =
                criarSimulacaoSimplificadaResidualRequest(PRAZOS, valorParcelaResidual);
        simulacaoSimplificadaResidualRequest.setItens(itens);
        ResponseEntity<SimulacaoSimplificadaResidualResponse> actual =
                simuladorSimplificadoController.getParcelaResidual( simulacaoSimplificadaResidualRequest);

        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        ReflectionAssert.assertReflectionEquals(expected, actual);
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCreditoFacilRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, null);
        simulacaoParcResidualRequest.setPlanoId(3300);
        simulacaoParcResidualRequest.setModalidade(Modalidade.CDC);

        PlanoService planoService = mock(PlanoService.class);
        when(planoService.getPlanoById(anyInt()))
                .thenReturn(getPlano());
        simuladorParcResidualController.setPlanoService(planoService);
        simuladorParcResidualController.setMotorTaxaPlanoService(motorTaxaPlanoService);
        Plano plano = new Plano();
        plano.setPermiteBalao(true);
        when(planoService.getPlanoById(any())).thenReturn(getPlano());
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(any())).thenReturn(getPlano());

        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CREDITO_FACIL);
        simulacaoSimplParcDesejadaRequest.setUfEmplacamento(null);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);
        //apagando valores que a variável da residual não possui para validar somente as parcelas
        actual.getBody().setValorRegistroContrato(null);
        actual.getBody().setValorCestaServico(null);
        actual.getBody().setValorTarifaCadastro(null);


        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));

        assertEquals(expected.getBody().getPrazos().size(), actual.getBody().getPrazos().size());
        ReflectionAssert.assertReflectionEquals(expected.getBody().getPrazos(), actual.getBody().getPrazos());
    }

    @Test
    public void shouldBeEqualsSimulacaoParcDesejadaCreditoInteligenteRequest() throws HttpStatusInvalidException {
        BigDecimal valorParcelaDesejada = BigDecimal.valueOf(1000);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest =
                criarSimulacaoParcResidualRequest(PRAZOS, null);
        simulacaoParcResidualRequest.setPlanoId(3300);
        simulacaoParcResidualRequest.setModalidade(Modalidade.CDC);

        PlanoService planoService = mock(PlanoService.class);
        when(planoService.getPlanoById(anyInt()))
                .thenReturn(getPlano());
        simuladorParcResidualController.setPlanoService(planoService);
        simuladorParcResidualController.setMotorTaxaPlanoService(motorTaxaPlanoService);
        Plano plano = new Plano();
        plano.setPermiteBalao(true);
        when(planoService.getPlanoById(any())).thenReturn(getPlano());
        when(motorTaxaPlanoService.getPlanoPorId(any())).thenReturn(new PlanoMotorTaxa());
        when(motorTaxaPlanoService.convertPlanoFromMotorTaxa(any())).thenReturn(getPlano());

        ResponseEntity<SimulacaoParcResidualResponse> expected =
                simuladorParcResidualController.getParcelaResidual(simulacaoParcResidualRequest);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(valorParcelaDesejada);
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.CREDITO_INTELIGENTE);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);
        //apagando valores que a variável da residual não possui para validar somente as parcelas
        actual.getBody().setValorRegistroContrato(null);
        actual.getBody().setValorCestaServico(null);
        actual.getBody().setValorTarifaCadastro(null);


        expected.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
        actual.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));

        ModalidadeSimplificada modalidade = ModalidadeSimplificada.fromValue("CREDITO-INTELIGENTE");
        simulacaoSimplParcDesejadaRequest.setModalidade(modalidade);

        assertEquals(expected.getBody().getPrazos().size(), actual.getBody().getPrazos().size());
        assertNotNull(simulacaoSimplParcDesejadaRequest.getModalidade().getValue());
        assertNotNull(simulacaoSimplParcDesejadaRequest.getModalidade().toString());

        ReflectionAssert.assertReflectionEquals(expected.getBody().getPrazos(), actual.getBody().getPrazos());
    }

    @Test(expected = EntityNotFoundException.class)
    public void shouldReturnErroEntityNotFoundExceptionLeasing() {

        when(motorTaxaPlanoService.getPlanoCicloToyota(any(), any(), any()))
                .thenThrow(EntityNotFoundException.class);

        SimulacaoSimplificadaDesejadaRequest simulacaoSimplParcDesejadaRequest =
                criarSimulacaoSimplificadaDesejadaRequest(new BigDecimal(1000));
        simulacaoSimplParcDesejadaRequest.setModalidade(ModalidadeSimplificada.LEASING);
        ResponseEntity<SimulacaoSimplificadaDesejadaResponse> actual =
                simuladorSimplificadoController.getParcelaDesejada( simulacaoSimplParcDesejadaRequest);

    }

	/*
	 * @Test public void shouldReturnErroResponse() { ResponseEntity<ErrorResponse>
	 * responseEntity =
	 * simuladorSimplificadoController.handleThirdPartyException(new
	 * ThirdPartyException("Error", null));
	 * assertEquals(HttpStatus.FAILED_DEPENDENCY, responseEntity.getStatusCode());
	 * assertEquals("Problema com um serviço externo. Error",
	 * responseEntity.getBody().getErros().get(0)); }
	 */


}
