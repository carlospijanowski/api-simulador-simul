package br.com.bancotoyota.services.simulador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;

import br.com.bancotoyota.services.simulador.services.*;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.DataBA;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {
        SimuladorOfertaPrestamistaController.class,
        DataCarencia.class,
        SimuladorParcDesejadaNovaController.class,
        ResidualService.class,
        SimuladorParcResidualController.class})
public class SimuladorOfertaPrestamistaControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CalculadoraServices calculadoraServices;

    @MockBean
    private DataBARepository repository;

    @MockBean
    private SimulacaoServices simulacaoServices;

	@MockBean
	private RedisRepository redisRepository;

    @MockBean
    private ParametrosDeSeguroPrestamistaService params;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private CalculadoraSeguroMecanicaServices seguroGarantiaServices;

    @MockBean
    private SeguroPrestamistaPlusServiceImpl seguroPrestamistaPlusService;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    private final String urlDesejada = "/ofertas/prestamistas/parcelas/desejada";

    private final String urlResidual = "/ofertas/prestamistas/parcelas/residual";

    private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();

    @Before
    public void setUp() throws Exception {
    	ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);

        when(repository.findAll()).thenReturn(
                Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
    }

    @Test
    public void quandoRequestVierSemNumeroDeParcelaParaResidualEntaoRetornaBadRequest() throws Exception {
        doThrow(new BusinessValidationException(Constants.PARCELAS, "É necessário especificar o número de parcelas"))
                .when(simulacaoServices).validaRequestSimulacao(any());

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simulacaoParcResidualRequest.setParcelas(new Integer[]{});
        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlResidual)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$[0].message", CoreMatchers.is("É necessário especificar o número de parcelas")))
                .andReturn();
    }

    @Test
    public void quandoRequestVierComMaisDeUmNumeroDeParcelaParaResidualEntaoRetornaBadRequest() throws Exception {
        doThrow(new BusinessValidationException(Constants.PARCELAS, "É permitido especificar somente um número de parcelas"))
                .when(simulacaoServices).validaRequestSimulacao(any());

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simulacaoParcResidualRequest.setParcelas(new Integer[]{12, 24});
        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlResidual)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$[0].message", CoreMatchers.is("É permitido especificar somente um número de parcelas")))
                .andReturn();
    }

    @Test
    public void quandoSeguroPrestamistaRetonarNullParaResidualEntaoRetornaNotFound() throws Exception {
        when(seguroPrestamistaPlusService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(null);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlResidual)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$.erros[0]", CoreMatchers.is("Array de Seguros não encontrada. Erro: null")))
                .andReturn();
    }

    @Test
    public void quandoRequestVierSemNumeroDeParcelaParaDesejadaEntaoRetornaBadRequest() throws Exception {
        doThrow(new BusinessValidationException(Constants.PARCELAS, "É necessário especificar o número de parcelas"))
                .when(simulacaoServices).validaRequestSimulacao(any());

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simulacaoParcResidualRequest.setParcelas(new Integer[]{});
        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlDesejada)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$[0].message", CoreMatchers.is("É necessário especificar o número de parcelas")))
                .andReturn();
    }

    @Test
    public void quandoRequestVierComMaisDeUmNumeroDeParcelaParaDesejadaEntaoRetornaBadRequest() throws Exception {
        doThrow(new BusinessValidationException(Constants.PARCELAS, "É permitido especificar somente um número de parcelas"))
                .when(simulacaoServices).validaRequestSimulacao(any());

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simulacaoParcResidualRequest.setParcelas(new Integer[]{12, 24});
        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlDesejada)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$[0].message", CoreMatchers.is("É permitido especificar somente um número de parcelas")))
                .andReturn();
    }

    @Test
    public void quandoSeguroPrestamistaRetonarNullParaDesejadaEntaoRetornaNotFound() throws Exception {
        when(seguroPrestamistaPlusService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(null);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        String json = mapper.writeValueAsString(simulacaoParcResidualRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(urlDesejada)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .headers(httpHeaders)
                .content(json);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.length()", CoreMatchers.is(1)))
                .andExpect(jsonPath("$.erros[0]", CoreMatchers.is("Array de Seguros não encontrada. Erro: null")))
                .andReturn();
    }

}
