package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.config.*;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.*;
import br.com.bancotoyota.services.simulador.repository.propostas.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.*;
import com.fasterxml.jackson.databind.*;
import org.apache.commons.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.runner.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.*;
import org.springframework.test.context.junit4.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;

import java.io.*;
import java.time.*;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {SimuladorParcResidualV2Controller.class, DataCarencia.class})
public class SimuladorParcResidualV2ControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculadoraParcelasResidual calculadoraParcelasResidual;

    @MockBean
    private LogService logService;

    @MockBean
    private PlanoService planoService;

    @MockBean
    private SimulacaoServices simulacaoServices;

    @MockBean
    private CalculadoraOfertas calculadoraOfertas;

    @MockBean
    private ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService;

    @MockBean
    private SeguroPrestamistaPlusServiceImpl seguroPrestamistaPlusService;

    @MockBean
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private SimulacaoParcResidual simulacaoParcResidual;

    @MockBean
    private DataBARepository repository;

    @MockBean
    private RedisRepository redisRepository;

    @MockBean
    private CalculadoraServices calculadoraServices;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @MockBean
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    @Test
    public void deveRetornarSimulacaoCorretamente() throws Exception {
        String request = lerArquivo(getClass().getClassLoader().getResourceAsStream("residual/basico-request.json"));

        ResultadoParcelaResidual resultadoMock = new ResultadoParcelaResidual(Collections.emptyList(), null);
        ControlesBA controleBA = new ControlesBA();
        controleBA.setDataAtualBA(LocalDateTime.of(2022, Month.SEPTEMBER, 9, 0, 0));
        when(redisRepository.getDataBA()).thenReturn(controleBA);
        when(repository.findAll()).thenReturn(Arrays.asList(new DataBA(LocalDate.of(2022, 9, 9))));

        doReturn(resultadoMock).when(calculadoraParcelasResidual).fazerSimulacao(
                any(SimulacaoParcResidualRequest.class),
                anyBoolean(),
                anyBoolean(),
                any(),
                anyBoolean()
        );

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/v2/parcelas/residual/simulacao")
                .header("perfil", "REPRESENTANTE")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(request);
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();

    }


    protected String lerArquivo(InputStream resource) throws IOException {
        return IOUtils.toString(resource, "UTF-8");
    }
}