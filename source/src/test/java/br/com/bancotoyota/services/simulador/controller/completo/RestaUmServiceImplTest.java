package br.com.bancotoyota.services.simulador.controller.completo;

import br.com.bancotoyota.services.simulador.beans.request.RestaUmRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaPrazoResponse;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.controller.SimuladorSimplificadoControllerBaseTest;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import br.com.bancotoyota.services.simulador.services.RestaUmService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.impl.DataCarenciaImpl;
import br.com.bancotoyota.services.simulador.services.impl.RestaUmServiceImpl;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class RestaUmServiceImplTest extends SimuladorSimplificadoControllerBaseTest {

    private RestaUmService restaUmService;

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

    private MocksParaTestesCompletos mocksParaTestesCompletos;

    @MockBean
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    @MockBean
    private SimuladorParcResidualController simuladorParcResidualController;

    @Before
    public void setUp() throws Exception {
        mocksParaTestesCompletos = new MocksParaTestesCompletos();
        mocksParaTestesCompletos.planoService = planoService;
        mocksParaTestesCompletos.motorTaxaPlanoService = motorTaxaPlanoService;
        mocksParaTestesCompletos.redisRepository = redisRepository;
        mocksParaTestesCompletos.prestamistaService = prestamistaService;
        mocksParaTestesCompletos.dataCarencia = new DataCarenciaImpl(dataBARepository,redisRepository);
        mocksParaTestesCompletos.setUp();
        mocksParaTestesCompletos.prepararMockDeSimulacaoServices(Arrays.asList(24, 36));

        restaUmService = new RestaUmServiceImpl(emplacamentoService,
                origemNegocioService, cestaServicoService, planoService, motorTaxaPlanoService,  clientService, simuladorParcResidualController, featureToggleConfig);
        ((RestaUmServiceImpl)restaUmService).setSimuladorParcDesejadaController(mocksParaTestesCompletos.desejadaController);
        ((RestaUmServiceImpl)restaUmService).setSimuladorParcDesejadaNovaController(mocksParaTestesCompletos.ncontroller);

        super.setup();
    }

    @Test
    public void opcaoCicloToyota() {
        SimulacaoSimplificadaDesejadaRequest parametros = criarSimulacaoSimplificadaDesejadaRequest(new BigDecimal(2000));
        ConfiguracaoPlano configuracao = new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, false, false, -1);
        RestaUmRequest request = new RestaUmRequest(parametros, configuracao);
        AlternativaResponse r = restaUmService.opcaoCicloToyota(request, "COORDENADOR", "token");
        assertNotNull(r);
        assertNotNull(r.getDadosAlternativa());
        assertNotNull(r.getDadosAlternativa().getCesta());
        assertNotNull(r.getDadosAlternativa().getRetorno());
        assertNotNull(r.getDadosAlternativa().getCestaItens());
        assertNotNull(r.getDadosAlternativa().getPlano());
        assertNotNull(r.getSimulacoes().getPrazos());
        assertEquals(2, r.getSimulacoes().getPrazos().size());
        assertEquals(1, r.getSimulacoes().getPrazos().stream().filter(p -> p.getFluxo() != null).count());
    }

    /**
     * Valida o caso de um plano selecionado com subsídio variável e plano original com subsídio fixo. Com isso a taxa
     * do plano original não é passada e vamos pegar a taxa do maior prazo.
     */
    @Test
    public void opcaoCicloToyotaPlanoSelecionadoComSubsidio() {

        Plano plano = new Plano("ciclo-toyota", "2709", "1", "CICLO LXT>40 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList(MARCA_ID)), new BigDecimal("40.000000"), new BigDecimal("100.000000"),
                new BigDecimal("0.00"), CINQUENTA_PERC, CARENCIA_MIN, CARENCIA_MAX, "CDC-TCM", false, "V", false,
                "AI_E_0KM", 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.OBRIGATORIO_BALAO,null,null,null);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(plano);
        when(motorTaxaPlanoService.getPlanoCicloToyota(any(), any(), any()))
                .thenReturn(plano);

        Subsidio subsidio = new Subsidio(EnumTipoSubsidio.VARIAVEL, BigDecimal.ZERO, BigDecimal.ZERO, NumberUtils.CEM,
                EnumTipoDistribuicao.VALOR_PERCENTUAL, new BigDecimal(5000), new BigDecimal(20), null);
        when(planoService.findSubsidio(2709))
                .thenReturn(subsidio);
        when(motorTaxaPlanoService.findSubsidio(2709))
                .thenReturn(subsidio);

        SimulacaoSimplificadaDesejadaRequest parametros = criarSimulacaoSimplificadaDesejadaRequest(new BigDecimal(2000));
        ConfiguracaoPlano configuracao = new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, false, true, 2709);
        RestaUmRequest request = new RestaUmRequest(parametros, configuracao);
        AlternativaResponse r = restaUmService.opcaoCicloToyota(request, "COORDENADOR", "token");
        assertNotNull(r);
        assertNotNull(r.getDadosAlternativa());
        assertNotNull(r.getDadosAlternativa().getCesta());
        assertNotNull(r.getDadosAlternativa().getRetorno());
        assertNotNull(r.getDadosAlternativa().getCestaItens());
        assertNotNull(r.getDadosAlternativa().getPlano());
        assertNotNull(r.getSimulacoes().getPrazos());
        assertEquals(2, r.getSimulacoes().getPrazos().size());
        assertEquals(1, r.getSimulacoes().getPrazos().stream().filter(p -> p.getFluxo() != null).count());
        assertEquals(new BigDecimal("1.21"), r.getSimulacoes().getPrazos().iterator().next().getTaxaMensal());
    }

    @Test
    public void quandoPrazoNMesesCDCEntaoRetornaOpcaoCicloToyotaEquivalente() {
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));

        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        when(simuladorParcResidualController.getParcelaResidualImpl(any(), anyList()))
                .thenReturn(new SimulacaoParcResidualResponse());

        /**
         Prazo menor que o mínimo. Então ele retorna o prazo mínimo.
         */
        RestaUmRequest request = buildRequestRestaUm(40);
        AlternativaPrazoResponse opcaoCicloToyotaAPrazo = restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");
        SimulacaoParcResidualResponse simulacoes = opcaoCicloToyotaAPrazo.getSimulacoes();
        SimulacaoParcResidualRequest simulacaoRequest = (SimulacaoParcResidualRequest) simulacoes.getRequest();

        assertEquals(40, simulacaoRequest.getPrazoDisponivelCiclo().get(0).getNumeroDeParcelas().intValue());
        assertEquals(new BigDecimal("40000").setScale(2, RoundingMode.HALF_EVEN), simulacaoRequest.getValorParcelaResidual());
    }

    @Test
    public void quandoPrazoNMesesMenorQueMinimoDoCicloEntaoRetornaOpcaoCicloToyotaComPrazoMinimo() {
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));

        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        when(simuladorParcResidualController.getParcelaResidualImpl(any(), anyList()))
                .thenReturn(new SimulacaoParcResidualResponse());

        /**
             Prazo menor que o mínimo. Então ele retorna o prazo mínimo.
        */
        RestaUmRequest request = buildRequestRestaUm(5);
        AlternativaPrazoResponse opcaoCicloToyotaAPrazo = restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");
        SimulacaoParcResidualResponse simulacoes = opcaoCicloToyotaAPrazo.getSimulacoes();
        SimulacaoParcResidualRequest simulacaoRequest = (SimulacaoParcResidualRequest) simulacoes.getRequest();

        assertEquals(12, simulacaoRequest.getPrazoDisponivelCiclo().get(0).getNumeroDeParcelas().intValue());
        assertEquals(new BigDecimal("40000").setScale(2, RoundingMode.HALF_EVEN), simulacaoRequest.getValorParcelaResidual());
    }

    @Test
    public void quandoPrazoNMesesMaiorQueMaximoDoCicloEntaoRetornaOpcaoCicloToyotaComPrazoMaximo() {
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));

        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        when(simuladorParcResidualController.getParcelaResidualImpl(any(), anyList()))
                .thenReturn(new SimulacaoParcResidualResponse());

        /**
         Prazo maior que o máximo. Então ele retorna o prazo máximo.
         */
        RestaUmRequest request = buildRequestRestaUm(49);
        AlternativaPrazoResponse opcaoCicloToyotaAPrazo = restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");
        SimulacaoParcResidualResponse simulacoes = opcaoCicloToyotaAPrazo.getSimulacoes();
        SimulacaoParcResidualRequest simulacaoRequest = (SimulacaoParcResidualRequest) simulacoes.getRequest();

        assertEquals(46, simulacaoRequest.getPrazoDisponivelCiclo().get(0).getNumeroDeParcelas().intValue());
        assertEquals(new BigDecimal("40000").setScale(2, RoundingMode.HALF_EVEN), simulacaoRequest.getValorParcelaResidual());
    }

    @Test
    public void quandoPrazoNMesesCDCComEntradaEResidualSuperiorAoValorBemEntaoRetonaOpcaoCicloToyotaComResidualAjustado() {
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));

        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        when(simuladorParcResidualController.getParcelaResidualImpl(any(), anyList()))
                .thenReturn(new SimulacaoParcResidualResponse());

        /**
            Bem: 100.000
            Entrada: 60.000
            Residual maximo (de acordo com plano): 50.000
            Residual esperado = 40.000
         */
        RestaUmRequest request = buildRequestRestaUm(22);
        AlternativaPrazoResponse opcaoCicloToyotaAPrazo = restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");

        SimulacaoParcResidualResponse simulacoes = opcaoCicloToyotaAPrazo.getSimulacoes();
        SimulacaoParcResidualRequest simulacaoRequest = (SimulacaoParcResidualRequest) simulacoes.getRequest();
        assertEquals(new BigDecimal("40000").setScale(2, RoundingMode.HALF_EVEN), simulacaoRequest.getValorParcelaResidual());
    }


    @Test(expected = EntityNotFoundException.class)
    public void quandoValorParcelasForNulaEntaoRetornaEntityNotFoundException() {
        //region Arrange objects
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));


        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        //endregion

        RestaUmRequest request = buildRequestRestaUm(22);
        request.getParametros().setParcelas(null);
        restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");
    }

    @Test(expected = EntityNotFoundException.class)
    public void quandoValorParcelasVazioEntaoRetornaEntityNotFoundException() {
        //region Arrange objects
        OrigemNegocio origemNegocio = new OrigemNegocio("215", "91919940439446", "VITORIA", "ES", "A", "91919940439446", "JUSCELEI SURIS SCHUINDT", "91919940439446", "ES - R.NORTE", true, true, true, true, true, true);
        List<CestaItem> cestaItems = Arrays.asList(new CestaItem(1, new BigDecimal("550"), 1, "CONFECCAO DE CADASTRO - PF", true),
                new CestaItem(3, new BigDecimal("320"), 1, "TARIFA DE ADITAMENTOS", false),
                new CestaItem(4, new BigDecimal("28.75"), 4, "TARIFA 2a VIA DOCUMENTOS", false),
                new CestaItem(7, new BigDecimal("28.75"), 4, "TARIFA DE DECLARACOES DIVERSAS", false));
        CestaServico cestaServico = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal("1100"), false, true, true, null);

        Plano planoCicloToyota = new Plano("ciclo-toyota", "4345", "4195", "PL20220825A", 2020, 2025, LocalDateTime.of(2022, 8, 7, 0, 0 ,0), new Plano.UsoPlanoObject(Arrays.asList("G"), null)
                , new BigDecimal("20.00"), new BigDecimal("99.999999"), new BigDecimal("20"), new BigDecimal("50"), 10, 190, "CDC-TCM", false, "", false, "AI_AF_E_0KM", 0, "12", "N", null,1, true, false, EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null);
        Retorno retorno = new Retorno("1", "1", "178");
        Estado ufEmplacamento = new Estado("ES", "Todas", new BigDecimal("364.59"), "500", LocalDateTime.of(2021, 9, 12, 0, 0, 0));


        when(origemNegocioService.getOrigemNegocio(anyString(), anyString()))
                .thenReturn(origemNegocio);
        when(cestaServicoService.getCestaServicoMaisCara(anyString(), any(), any(), anyString(), anyString()))
                .thenReturn(cestaServico);
        when(cestaServicoService.getDespesas(any(), anyString()))
                .thenReturn(cestaItems);
        when(planoService.getPlanoCicloToyota(any(), any(), any(), anyString()))
                .thenReturn(planoCicloToyota);

        when(planoService.getRetorno(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(motorTaxaPlanoService.getRetorno(anyString(), anyString(), anyString()))
                .thenReturn(retorno);
        when(emplacamentoService.getEstado(any(), anyString()))
                .thenReturn(ufEmplacamento);

        when(simuladorParcResidualController.carregarPrazos(any()))
                .thenReturn(carregarPrazos());
        //endregion

        RestaUmRequest request = buildRequestRestaUm(22);
        request.getParametros().setParcelas(new Integer[]{});
        restaUmService.opcaoCicloToyotaAPrazo(request, "REPRESENTANTE", "");
    }

    private List<Prazo> carregarPrazos() {
        return Arrays.asList(new Prazo(12, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(13, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(14, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(15, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(16, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(17, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(18, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(19, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(20, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(21, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(22, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(23, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(24, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(25, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(25, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(26, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(27, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(28, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(29, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(30, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(31, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(32, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(33, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(34, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(35, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(36, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(37, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(38, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(39, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(40, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(41, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(42, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(43, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(44, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(45, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null),
                new Prazo(46, new BigDecimal("1.440000"), new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("99.999999"),null)
        );
    }

    private RestaUmRequest buildRequestRestaUm(Integer nMeses) {
        RestaUmRequest request = new RestaUmRequest();
        ConfiguracaoPlano configuracaoPlano = new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM, false, false, 4176);
        SimulacaoSimplificadaDesejadaRequest parametros = new SimulacaoSimplificadaDesejadaRequest();
        parametros.setValorBem(new BigDecimal("100000"));
        parametros.setValorEntrada(new BigDecimal("60000"));
        parametros.setUfEmplacamento(UfEmplacamento.ES);
        parametros.setCnpjOrigemNegocio("91919940439446");
        parametros.setMarcaId("002");
        parametros.setModeloId("002182-2&03");
        parametros.setAnoModelo(2023);
        parametros.setSeguroAuto(BigDecimal.ZERO);
        parametros.setItens(new ArrayList<>());
        parametros.setSvpRemovido(false);
        parametros.setSpfRemovido(false);
        parametros.setParcelas(new Integer[]{nMeses});

        request.setParametros(parametros);
        request.setConfiguracao(configuracaoPlano);
        return request;
    }


}