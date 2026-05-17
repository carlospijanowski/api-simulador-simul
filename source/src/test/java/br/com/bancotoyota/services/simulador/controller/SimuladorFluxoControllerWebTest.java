package br.com.bancotoyota.services.simulador.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualSimplesRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {SimuladorParcResidualController.class, DataCarencia.class})
public class SimuladorFluxoControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalculadoraServices calculadoraServices;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private CalculadoraSeguroMecanicaServices seguroGarantiaServices;

    @MockBean
    private SimulacaoServices simulacaoServices;

    @MockBean
    private RedisRepository redisRepository;

    @MockBean
    private DataBARepository repository;

    @MockBean
    private ParametrosDeSeguroPrestamistaService params;

    @Autowired
    private ObjectMapper mapper;


    @MockBean
    private SeguroPrestamistaPlusServiceImpl seguroPrestamistaPlusService;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @Before
    public void setUp() throws Exception {
    	ControlesBA controleBA = new ControlesBA();
    	controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
        when(repository.findAll()).thenReturn(
                Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
        when(redisRepository.getDataBA()).thenReturn(controleBA);
    }

    @Test
    public void testaParametroParcelas() throws Exception {
        List<Prazo> prazoList = new ArrayList<>();
        prazoList.add(new Prazo(12, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));
        prazoList.add(new Prazo(24, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));
        prazoList.add(new Prazo(36, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal(20), new BigDecimal(50),null));

        when(simulacaoServices.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> prazoList);
        Integer prazo = 36;
        Integer[] parcelas = { prazo };
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
        when(redisRepository.findTaxasIOF("iof:pf")).thenReturn(taxasIOF);
        when(redisRepository.findTaxasIOF("iof:pj")).thenReturn(taxasIOF);

        when(simulacaoServices.getTaxasIOF(any(), isNull())).then(i -> taxasIOF);
        SimulacaoParcResidualSimplesRequest request = new SimulacaoParcResidualSimplesRequest();
        request.setValorUfEmplacamento(new BigDecimal(250));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorCestaServicos(new BigDecimal(250));
        request.setValorTC(new BigDecimal(250));
        request.setTaxaMes(new BigDecimal(2));
        request.setDataPrimeiroVencimento(LocalDate.of(2019,5,23).plusDays(30));
        request.setPlanoId(2345);
        request.setValorBem(new BigDecimal(100000));
        request.setRetorno("1");
        request.setTipoPessoa("pessoa-fisica");
        request.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista(true, false, true, false, true, null, null,true,true, "", Arrays.asList(),null));
        request.setValorParcelaResidual(new BigDecimal(20000));
        request.setCarenciaDoPlano(new Carencia(10,180));

        request.setParcelas(null);
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/fluxo/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().is5xxServerError()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testaParametroParcelasInvalido() throws Exception {

        SimulacaoParcResidualSimplesRequest request = new SimulacaoParcResidualSimplesRequest();
        request.setValorUfEmplacamento(new BigDecimal(250));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorCestaServicos(new BigDecimal(250));
        request.setValorTC(new BigDecimal(250));
        request.setDataPrimeiroVencimento(LocalDate.of(2019,5,23).plusDays(30));
        request.setPlanoId(2345);
        request.setValorBem(new BigDecimal(100000));
        request.setRetorno("1");
        request.setTipoPessoa("pessoa-fisica");
        request.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista(true, false, true, false, true, null, null,true,true, "", Arrays.asList(),null));
        request.setValorParcelaResidual(new BigDecimal(20000));

        request.setParcelas(new Integer[]{1234});
        String json = mapper.writeValueAsString(request);
        System.out.println(json);
        json = json.replace("\"parcelas\":[1234]","\"parcelas\":[\"alo\"]}");
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/fluxo/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("parcelas.null"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("campo inválido"))).
                andExpect(jsonPath("$[0].value", CoreMatchers.is("alo"))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }
}
