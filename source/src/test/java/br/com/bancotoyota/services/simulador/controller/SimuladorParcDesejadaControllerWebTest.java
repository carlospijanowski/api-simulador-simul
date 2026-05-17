package br.com.bancotoyota.services.simulador.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import br.com.bancotoyota.services.simulador.services.*;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.DataBA;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.CalculadoraServices;
import br.com.bancotoyota.services.simulador.services.DataCarencia;
import br.com.bancotoyota.services.simulador.services.ParametrosDeSeguroPrestamistaService;
import br.com.bancotoyota.services.simulador.services.ResidualService;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {
        SimuladorParcDesejadaController.class,
        DataCarencia.class,
        SimuladorParcDesejadaNovaController.class,
        ResidualService.class,
        SimuladorParcResidualController.class})
public class SimuladorParcDesejadaControllerWebTest {
    @Mock
    private RestTemplate restTemplate;
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
    private SeguroPrestamistaPlusServiceImpl seguroPrestamistaPlusService;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private CalculadoraSeguroMecanicaServices seguroGarantiaServices;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @Before
    public void setUp() throws Exception {
    	ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);

        when(repository.findAll()).thenReturn(
                Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
        when(simulacaoServices.getTaxasIOF(any(), isNull())).then(i -> taxasIOF);
    }

    @Test
    public void testeDesejadaEntradaMuitoAlta() throws Exception {
        when(simulacaoServices.getParcelas(any(), any(), any(), isNull(), isNull())).then(i -> SimuladorParcDesejadaControllerTest.dadosDoPlano(i.getArgument(2)));

        SimulacaoParcDesejadaRequest request = new SimulacaoParcDesejadaRequest();
        request.setSeguroAuto(BigDecimal.TEN);

        request.setDataPrimeiroVencimento(LocalDate.of(2019,5,23).plusDays(30));
        request.setValorBem(new BigDecimal("100000"));
        request.setValorTC(new BigDecimal(250));
        request.setValorCestaServicos(new BigDecimal(250));
        request.setValorEntrada(new BigDecimal(81000));
        request.setValorUfEmplacamento(new BigDecimal(250));
        request.setValorParcelaDesejada(new BigDecimal(500));
        request.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista(true, false, true, false, true, null, null,true,true, "", Arrays.asList(),null));
        request.setRetorno("1");
        request.setCarenciaDoPlano(new Carencia(10,180));

        request.setTipoPessoa("pessoa-fisica");

        request.setPlanoId(2705);

        String json = mapper.writeValueAsString(request);
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("valor-entrada"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("Valor da entrada muito alto. Entrada mais valor mínimo para a " +
                        "parcela residual não pode ser maior que o valor do bem."))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeDesejadaPlanoVazio() throws Exception {
        SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1000));

        int id = 0;
        request.setPlanoId(id);

        String json = mapper.writeValueAsString(request);
        json = json.replace("\"plano-id\":0","");
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andExpect(jsonPath("$.length()", CoreMatchers.is(1))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeDesejadaPlanoInvalido() throws Exception {
        SimulacaoParcDesejadaRequest request =criarRequest(new BigDecimal(1000));

        int id = 2705;
        request.setPlanoId(id);

        String json = mapper.writeValueAsString(request);
        json = json.replace(":"+id+",",":\"alo\",");
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("plano-id"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("campo inválido"))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeDesejadaDataInvalida() throws Exception {
        SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(1000));
        String json = mapper.writeValueAsString(request);
        String dataStr = request.getDataPrimeiroVencimento().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String original = "\"data-primeiro-vencimento\":\"" + dataStr + "\",";
        json = json.replace(original,"\"data-primeiro-vencimento\":\"alo\",");
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("data-primeiro-vencimento"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("campo inválido"))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }


    @Test
    public void testeValorParcelaDesejadaNaoInformado() throws Exception {
        SimulacaoParcDesejadaRequest request = criarRequest(null);

        String json = mapper.writeValueAsString(request);

        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("valor-parcela-desejada"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("campo não informado"))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeValorParcelaDesejadaInvalido() throws Exception {
        SimulacaoParcDesejadaRequest request = criarRequest(BigDecimal.TEN);

        String json = mapper.writeValueAsString(request);

        System.out.println(json);
        json = json.replace("\"valor-parcela-desejada\":10", "\"valor-parcela-desejada\":\"batata\"");
        System.out.println(json);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("valor-parcela-desejada"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("campo inválido"))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testDataPrimeiroVencimentoNoPassado() throws Exception{
        SimulacaoParcDesejadaRequest request = criarRequest(new BigDecimal(2500));
        request.setDataPrimeiroVencimento(LocalDate.of(2019,02,17));

        String json = mapper.writeValueAsString(request);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).
                andExpect(jsonPath("$[0].field", CoreMatchers.is("data-primeiro-vencimento"))).
                andExpect(jsonPath("$[0].message", CoreMatchers.is("Data do primeiro vencimento não pode ser no passado (data de cálculo 2019-02-18)."))).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeValidacaoTodosOsCampos() throws Exception {
        String json = "{}";

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/desejada/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        // o erro sobre a falta do campo do valor da parcela desejada não é mais validado com anotação pois
        // agora o request usado em todas as simulações é o mesmo
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).
                andExpect(jsonPath("$.length()", CoreMatchers.is(1))).andReturn();
        System.out.println(result.getResponse().getContentAsString());

    }


    private SimulacaoParcDesejadaRequest criarRequest(BigDecimal valor_parcela_desejada) {
        BigDecimal valor_uf_emplacamento = new BigDecimal(250.00);
        BigDecimal valor_cesta_servicos = new BigDecimal(200.00);
        BigDecimal valor_taxa_cadastro = new BigDecimal(130.00);
        String retorno = "0";
        Integer plano_id = 2752;
        String tipoPessoa = "pessoa-fisica";
        BigDecimal valor_bem = new BigDecimal(100000);
        BigDecimal valor_entrada = new BigDecimal(40000);
        BigDecimal valor_seguro_franquia = new BigDecimal(12000);

        LocalDate dataPrimeiroVencimento = LocalDate.of(2019,5,23).plusDays(30);

        SimulacaoParcDesejadaRequest request = new SimulacaoParcDesejadaRequest();

        request.setValorUfEmplacamento(valor_uf_emplacamento);
        request.setValorCestaServicos(valor_cesta_servicos);
        request.setValorTC(valor_taxa_cadastro);
        request.setRetorno(retorno);
        request.setPlanoId(plano_id);
        request.setTipoPessoa(tipoPessoa);
        request.setValorBem(valor_bem);
        request.setValorEntrada(valor_entrada);
        request.setValorParcelaDesejada(valor_parcela_desejada);
        request.setVlrSeguroFranquia(valor_seguro_franquia);

        request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
        request.setCarenciaDoPlano(new Carencia(10,180));
        ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista = new ParametrosDeSeguroPrestamista(false, false, true, true, true, null, null,true,true, "", Arrays.asList(),null);
        request.setParametrosDeSeguroPrestamista(parametrosDeSeguroPrestamista);
        System.out.println("\n-----  PARAMETROS ------------------------------");
        System.out.println("Valor do Bem              : " + valor_bem);
        System.out.println("Valor de Entrada          : " + valor_entrada);
        System.out.println("Valor da Parcela Desejada : " + valor_parcela_desejada);
        System.out.println("-----------------------------------");
        return request;
    }
}
