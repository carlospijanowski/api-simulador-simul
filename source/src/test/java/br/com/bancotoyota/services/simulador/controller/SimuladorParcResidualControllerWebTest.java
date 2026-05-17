package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.ValidacaoRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualControllerValidacaoTest.criarValidacaoRequest;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {
        SimuladorParcResidualController.class,
        DataCarencia.class,
        ParametrosDeSeguroPrestamistaService.class,
        LogService.class
    }
)
public class SimuladorParcResidualControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimulacaoServices simulacaoServices;

    @MockBean
    private DataBARepository repository;

    @MockBean
    private RedisRepository redisRepository;

    @MockBean
    private CalculadoraServices calculadoraServices;

    @MockBean
    private CalculadoraSeguroMecanicaServices seguroGarantiaServices;

    @MockBean
    private ClientService clientService;

    @MockBean
    private LogService logService;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private SeguroPrestamistaPlusServiceImpl prestamistaService;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @Before
    public void setUp() throws Exception {
        prepararMocks();
        ControlesBA controleBA = new ControlesBA();
		controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
		when(redisRepository.getDataBA()).thenReturn(controleBA);
        when(repository.findAll()).thenReturn(
                Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
        doNothing().when(motorTaxaPlanoService).applyFatorRating(any());
    }

    @Test
    public void testaParametrosDoSeguroOk() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        validarRequest(request, status().isOk());
    }

    @Test
    public void testaPessoaJuridica() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setTipoPessoa(TipoPessoa.PESSOA_JURIDICA.getValue().toLowerCase());
        // no caso de pessoa jurídica os parametros do seguro prestamista não são obrigatórios
        request.setParametrosDeSeguroPrestamista(null);
        validarRequest(request, status().isOk());
    }

    @Test
    public void testaTipoPessoaInvalido() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setTipoPessoa("valor-invalido");
        // no caso de pessoa jurídica os parametros do seguro prestamista não são obrigatórios
        request.setParametrosDeSeguroPrestamista(null);
        validarRequest(request, status().isBadRequest());
    }

    @Test
    public void testaPessoaFisicaSemParametrosDeSeguroPrestamista() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        // no caso de pessoa jurídica os parametros do seguro prestamista não são obrigatórios
        request.setParametrosDeSeguroPrestamista(null);
        validarRequest(request, status().isBadRequest());
    }

    @Test
    public void testaValorDoBem() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorBem(null);
        validarRequest(request, status().is5xxServerError());
    }

    @Test
    public void testaTaxaCadastro() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorTC(null);
        validarRequest(request, status().is5xxServerError());
    }

    @Test
    public void testaCestaServicos() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorCestaServicos(null);
        validarRequest(request, status().is5xxServerError());
    }

    @Test
    public void testaValorEntrada() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorEntrada(null);
        validarRequest(request, status().is5xxServerError());
    }

    @Test
    public void testaUfEmplacamento() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorUfEmplacamento(null);
        validarRequest(request, status().is5xxServerError());
    }

    @Test
    public void testaPlanoId() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setPlanoId(-1);
        validarRequest(request, status().is2xxSuccessful());
    }

    @Test
    public void testaDataPrimeiroVencimento() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setTrintaDiasAposVencimento(false);
        request.setDataPrimeiroVencimento(LocalDate.of(2019,02, 18));
        validarRequest(request, status().isBadRequest());
    }

    @Test
    public void testaDataPrimeiroVencimentoNull() throws Exception {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setDataPrimeiroVencimento(null);
        request.setTrintaDiasAposVencimento(false);
        validarRequest(request, status().isBadRequest());
    }

    private void validarRequest(SimulacaoParcResidualRequest request, ResultMatcher resultMatcher) throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/residual/simulacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        ResultActions result = mockMvc.perform(requestBuilder);
        result.andExpect(resultMatcher);
    }

    private SimulacaoParcResidualRequest criarRequest() {

        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setDataPrimeiroVencimento(null);
        request.setTrintaDiasAposVencimento(true);
        request.setValorBem(new BigDecimal("100000"));
        request.setValorTC(new BigDecimal(250));
        request.setValorCestaServicos(new BigDecimal(250));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorUfEmplacamento(new BigDecimal(250));
        request.setParametrosDeSeguroPrestamista(new ParametrosDeSeguroPrestamista(true, false, true, false, true, null, null,true,true, "", Arrays.asList(),null));
        request.setRetorno("1");
        request.setValorParcelaResidual(new BigDecimal(25000));
        request.setCarenciaDoPlano(new Carencia(10,180));
        request.setTipoPessoa("pessoa-fisica");
        request.setIsentaIOF("N");

        request.setPlanoId(2705);
        return request;
    }

    private void prepararMocks() throws Exception {
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");
        when(simulacaoServices.getTaxasIOF(any(), anyString())).then(i -> taxasIOF);

        List<Prazo> prazoList = new ArrayList<>();
        prazoList.add(new Prazo(12, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));
        prazoList.add(new Prazo(24, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));
        prazoList.add(new Prazo(36, new BigDecimal("1.308033"), new BigDecimal("20.00"),
                new BigDecimal("40.00"), new BigDecimal(20), new BigDecimal(50),null));

        when(simulacaoServices.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> prazoList);

        BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
        Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
                new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
        Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
                new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);

        List<Seguro> seguros = Arrays.asList(spf, svp);
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));
    }

    @Test
    public void testaTaxaMaximaSubsidio() throws Exception {
        when(simulacaoServices.getSubsidio(2806)).thenReturn(Subsidio.builder().tipoSubsidio(EnumTipoSubsidio.VARIAVEL).build());
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/taxa-maxima-subsidio")
                .param("plano-id","2806")
                .param("percentual-entrada","25")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testaTaxaMaximaSubsidioSemPlanoId() throws Exception {
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/parcelas/taxa-maxima-subsidio")
                .param("plano-id2","2806")
                .param("percentual-entrada","25")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isBadRequest()).andExpect(content().json(
                "[{\"field\":\"plano-id\",\"message\":\"campo não informado\"}]")
        ).andReturn();


        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    public void testeValidacaoDaPropostaEmCasoDeErroHttp() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        when(clientService.getValoresComprometidos(anyString())).thenThrow(
                HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR,"internal error", headers,
                        "erro interno mesmo".getBytes(), null));

        ValidacaoRequest validacaoRequest = criarValidacaoRequest(EnumTipoValidacao.VALIDACAO_COMPLETA);
        String json = mapper.writeValueAsString(validacaoRequest);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/validacao")
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header("token","token")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult result = mockMvc.perform(requestBuilder).andExpect(status().isInternalServerError()).andReturn();
    }
}
