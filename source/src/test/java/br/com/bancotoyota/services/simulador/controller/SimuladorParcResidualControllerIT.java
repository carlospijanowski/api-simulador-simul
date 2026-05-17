package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraSeguroMecanicaServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Ignore
public class SimuladorParcResidualControllerIT {

    private SimuladorParcResidualController controller;

    private CalculadoraServices calculadoraServices = new CalculadoraServicesImpl();

    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices = new CalculadoraSeguroMecanicaServicesImpl();

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);
    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private DataBARepository repository;

    private DataCarencia dataCarencia;

    @Autowired
    private SimulacaoServices simulacaoServices;

    @Autowired
    private ParametrosDeSeguroPrestamistaService params;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private SeguroPrestamistaPlusServiceImpl prestamistaService;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @Before
    public void setUp() {
        dataCarencia = new DataCarenciaImpl(repository,redisRepository);

        controller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
                new BigDecimal(10), params, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
    }

    protected String lerArquivo(InputStream resource) throws IOException {
        return IOUtils.toString(resource, "UTF-8");
    }


    @Test
    public void test() throws Exception {

        SimulacaoParcResidualRequest request = mapper.readValue(
                lerArquivo(getClass().getClassLoader().getResourceAsStream("request-subsidio-loop.json")), SimulacaoParcResidualRequest.class);
        ResponseEntity<SimulacaoParcResidualResponse> response = controller.getParcelaResidual(request);
        Assert.assertNotNull(response);
    }
}
