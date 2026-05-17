package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualSimplesRequest;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoFluxoResponse;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.impl.CalculadoraServicesImpl;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.Times;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SimuladorFluxoControllerTest {

    private SimuladorParcResidualController controller;

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

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
    private SeguroPrestamistaPlusServiceImpl prestamistaService;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private FatorSimulacao fatorSimulacao;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        dataCarencia = new DataCarenciaImpl(repository,redisRepository);
        controller = new SimuladorParcResidualController(calculadoraServices, simulacaoServices, dataCarencia,
                BigDecimal.TEN, params, false, prestamistaService, featureToggleConfig, seguroFranquiaServices, fatorSimulacao, calculadoraSeguroMecanicaServices);
        when(repository.findAll()).thenReturn(
                Arrays.asList(new DataBA(LocalDate.of(2019, 02, 18))));
        ControlesBA controleBA = new ControlesBA();
        controleBA.setDataAtualBA(LocalDateTime.of(2019, Month.FEBRUARY, 18, 0, 0));
        when(redisRepository.getDataBA()).thenReturn(controleBA);
    }

    @Test
    public void getFluxoSimulacao() throws Exception {
        List<Prazo> prazoList = prepararMockDeSimulacaoServices();
        int[] parcela = {12};
        assertEquals(prazoList.size(), parcela.length);

        SimulacaoParcResidualSimplesRequest request = criarRequest(parcela.length, 98000);
        ResponseEntity<SimulacaoFluxoResponse> response = controller.getFluxoSimulacao(request);
        SimulacaoFluxoResponse simulacaoFluxoResponse = response.getBody();
        List<Fluxo> list = simulacaoFluxoResponse.getFluxoList();

        Assert.assertFalse(list.isEmpty());

        verify(simulacaoServices).getTaxasIOF(any(), isNull());
        verify(simulacaoServices).getParcelas(anyInt(), any(), any(), isNull(), isNull());
        verify(prestamistaService, new Times(parcela.length)).getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean());
        verify(simulacaoServices).getSubsidio(anyInt());
        verifyNoMoreInteractions(simulacaoServices);
    }

    @Test(expected = BusinessValidationException.class)
    public void testaCarenciaMaximaDataVencimento(){
        SimulacaoParcResidualSimplesRequest request = criarRequest(12, 98000);
        request.setDataPrimeiroVencimento(LocalDate.of(2019,5,23).plusDays(190));
        request.setCarenciaDoPlano(new Carencia(10, 180));
        assertFalse("data do primeiro vencimento deve ser menor que o maxima da carencia",dataCarencia.isDentroCarencia(request, dataCarencia.getDataCalculo()));
    }

    @Test(expected = BusinessValidationException.class)
    public void testaCarenciaMinimaDataVencimento(){
        SimulacaoParcResidualSimplesRequest request = criarRequest(12, 98000);
        request.setDataPrimeiroVencimento(dataCarencia.getDataCalculo().plusDays(5));
        request.setCarenciaDoPlano(new Carencia(10, 180));
        assertFalse("data do primeiro vencimento deve ser maior que o mínimo da carência",dataCarencia.isDentroCarencia(request, dataCarencia.getDataCalculo()));
    }

    @Test(expected = BusinessValidationException.class)
    public void testaDataVencimentoForaDaCarencia(){
        SimulacaoParcResidualSimplesRequest request = criarRequest(12, 98000);
        request.setDataPrimeiroVencimento(dataCarencia.getDataCalculo().plusDays(5));
        request.setCarenciaDoPlano(new Carencia(10, 15));
        assertTrue("A data do primeiro vencimento estã invãlida. Por favor, selecione uma nova data.",dataCarencia.isDentroCarencia(request, dataCarencia.getDataCalculo()));
    }

    @Test
    public void testaValorPrestamistaComSeguroAuto() {
        List<Prazo> prazoList = prepararMockDeSimulacaoServices();
        int[] parcela = {12};
        assertEquals(prazoList.size(), parcela.length);
        SimulacaoParcResidualSimplesRequest request = criarRequest(parcela.length, 98000);
        request.setSeguroAuto(new BigDecimal(0));
        ResponseEntity<SimulacaoFluxoResponse> response = controller.getFluxoSimulacao(request);
        SimulacaoFluxoResponse simulacao = response.getBody();
        List<Fluxo> list = Objects.requireNonNull(simulacao).getFluxoList();

        Fluxo fluxo = list.get(1);
        BigDecimal valorDaParcela = fluxo.getValorParcela();

        request.setSeguroAuto(new BigDecimal(3000));
        response = controller.getFluxoSimulacao(request);
        simulacao = response.getBody();
        list = simulacao.getFluxoList();
        fluxo = list.get(1);
        BigDecimal valorDaParcelaComSeguroAuto = fluxo.getValorParcela();
        assertTrue("O valor da parcela com o seguro auto deve ser maior que a parcela sem o seguro informado", valorDaParcelaComSeguroAuto.compareTo(valorDaParcela) > 0);
    }

    @Test
    public void testaValorDaUltimaParcela() throws Exception {
        List<Prazo> prazoList = prepararMockDeSimulacaoServices();
        int[] parcela = {12};
        assertEquals(prazoList.size(), parcela.length);
        SimulacaoParcResidualSimplesRequest request = criarRequest(parcela.length, 98000);
        ResponseEntity<SimulacaoFluxoResponse> response = controller.getFluxoSimulacao(request);
        SimulacaoFluxoResponse simulacao = response.getBody();
        List<Fluxo> list = Objects.requireNonNull(simulacao).getFluxoList();

        Fluxo fluxo = list.get(parcela.length);
        BigDecimal valorDaUltimaParcela = fluxo.getValorParcela().add(request.getValorParcelaResidual());
        assertEquals("O valor da ultima parcela deve ser a parcela + residual", 0, valorDaUltimaParcela.compareTo(valorDaUltimaParcela));
    }

    @Test
    public void testaFluxoInterna() throws Exception {
        List<Prazo> prazoList = prepararMockDeSimulacaoServices();
        int[] parcela = {12};
        assertEquals(prazoList.size(), parcela.length);
        SimulacaoParcResidualSimplesRequest request = criarRequest(parcela.length, 98000);
        request.setOrigemNegocio(new OrigemNegocio());
        request.setCpfProponente("cpf");
        ParametrosDeSeguroPrestamista p = new ParametrosDeSeguroPrestamista();
        when(params.getParametros(any(), anyString(), anyString(), any(), isNull())).thenReturn(p);
        ResponseEntity<SimulacaoFluxoResponse> response = controller.getFluxoSimulacao("token", request);
        SimulacaoFluxoResponse simulacao = response.getBody();
        List<Fluxo> list = Objects.requireNonNull(simulacao).getFluxoList();

        Fluxo fluxo = list.get(parcela.length);
        BigDecimal valorDaUltimaParcela = fluxo.getValorParcela().add(request.getValorParcelaResidual());
        assertEquals("O valor da ultima parcela deve ser a parcela + residual", 0, valorDaUltimaParcela.compareTo(valorDaUltimaParcela));
    }

    private List<Prazo> prepararMockDeSimulacaoServices() {
        // { "base-calculo": "365.000000", "iof-aa": "0.030000", "iof-ad": "0.003800" }
        TaxasIOF taxasIOF = new TaxasIOF("365.000000", "0.030000", "0.003800");

        //Plano 2752
        //"{ \"prazo\": \"12\", \"taxa-mes\": \"1.480427\", \"perc-min-balao\": \"20.00\", \"perc-max-balao\": \"50.00\" }"
        List<Prazo> prazoList = new ArrayList<>();
        BigDecimal limiteDeFinanciamento = new BigDecimal(90000);
        prazoList.add(new Prazo(12, new BigDecimal("1.480427"), new BigDecimal("20.00"),
                new BigDecimal("50.00"), new BigDecimal(20), new BigDecimal(50),null));

        Seguro spf = new Seguro(TipoDeSeguroPrestamista.SPF, new BigDecimal(2500), limiteDeFinanciamento,
                new BigDecimal(450000.00), new BigDecimal("0.029000"), 18, 65);
        Seguro svp = new Seguro(TipoDeSeguroPrestamista.SVP, BigDecimal.ZERO, limiteDeFinanciamento,
                new BigDecimal(990000.00), new BigDecimal("0.012000"), 18, 65);

        spf.setOrdenacao(1);
        svp.setOrdenacao(2);

        List<Seguro> seguros = Arrays.asList(spf, svp);
        when(simulacaoServices.getTaxasIOF(any(), isNull())).then(i -> taxasIOF);
        when(simulacaoServices.getParcelas(anyInt(), any(), any(), isNull(), isNull())).then(i -> prazoList);
        when(prestamistaService.getSegurosPrestamista(any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(new SeguroPrestamistaPlusResponse(seguros, seguros.get(1)));

        return prazoList;
    }

    private SimulacaoParcResidualSimplesRequest criarRequest(int parcelas, double valorDoBem) {
        BigDecimal valor_uf_emplacamento = new BigDecimal(250.00);
        BigDecimal valor_cesta_servicos = new BigDecimal(200.00);
        BigDecimal valor_taxa_cadastro = new BigDecimal(130.00);
        String retorno = "1";
        Integer plano_id = 2752;
        String tipoPessoa = "pessoa-fisica";
        BigDecimal valor_bem = new BigDecimal(valorDoBem);
        BigDecimal valor_entrada = new BigDecimal(62910.00);
        BigDecimal valor_parcela_residual = new BigDecimal(31455.00);
        LocalDate dataPrimeiroVencimento = LocalDate.of(2019,5,23).plusDays(10);
        List<ItemFinanciavel> itens = new ArrayList<>();
        itens.add(getItemFinanciavel(1, BigDecimal.valueOf(200)));
        SimulacaoParcResidualSimplesRequest request = new SimulacaoParcResidualSimplesRequest();

        List<ItemFinanciavel> items = new ArrayList<>();
        items.add(getItemFinanciavel(2,new BigDecimal(100)));
        items.add(getItemFinanciavel(3,new BigDecimal(200)));
        request.setValorUfEmplacamento(valor_uf_emplacamento);
        request.setItens(items);
        request.setValorCestaServicos(valor_cesta_servicos);
        request.setValorTC(valor_taxa_cadastro);
        request.setRetorno(retorno);
        request.setPlanoId(plano_id);
        request.setTipoPessoa(tipoPessoa);
        request.setValorBem(valor_bem);
        request.setValorEntrada(valor_entrada);
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setValorParcelaResidual(valor_parcela_residual);
        request.setDataPrimeiroVencimento(dataPrimeiroVencimento);
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setItens(itens);
        request.setParcelas(new Integer[]{parcelas});
        ParametrosDeSeguroPrestamista parametroDeSeguro = new ParametrosDeSeguroPrestamista(false, false, true, false, true, null, null,true,true, "", Arrays.asList(),null);
        request.setParametrosDeSeguroPrestamista(parametroDeSeguro);
        request.setSeguroAuto(new BigDecimal(3000));
        return request;
    }

    private ItemFinanciavel getItemFinanciavel(int codigo, BigDecimal valor){
        ItemFinanciavel i = new ItemFinanciavel();
        i.setCodigo(codigo);
        i.setValor(valor);
        return i;
    }

}
