package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.request.FatorSimulacao;
import br.com.bancotoyota.services.simulador.beans.response.Modalidade;
import br.com.bancotoyota.services.simulador.beans.response.Taxa;
import br.com.bancotoyota.services.simulador.beans.response.TaxasDetalhe;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.beans.response.TipoPessoa;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.ErrorParser;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class MotorTaxaPlanoServiceTest {

    @Value("${api.motor-taxa.url}")
    private String apiMotorTaxaUrl;

    @Value("${planos.obo.filter}")
    private String oboFilterExpressionStr;

    @Value("${planos.filter}")
    private String filterExpressionStr;

    boolean filtroIdModalidadeEnabled;

    private static final String MODALIDADE = "ciclo-toyota";
    private static final String MODALIDADE_CDC = "cdc";
    private static final String TIPO_PLANO = "CDC-TCM";
    private static final String INDICADOR_FAIXA_ANO = "AI_E_0KM";
    private static final String CNPJ_ORIGEM_NEGOCIO = "05868140232815";
    private static final String REGIAO = "CANOPUS";
    private static final String MARCA = "002";
    private static final String MODELO = "001265";
    private static final LocalDateTime DATA_INICIAL_VIGENCIA =
            LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER);
    private static final BigDecimal TRINTA = new BigDecimal("30.00");
    private static final BigDecimal QUARENTA = new BigDecimal("40.00");
    private static final BigDecimal CINQUENTA = new BigDecimal("50.00");
    private static final BigDecimal CEM = new BigDecimal("100.000000");
    private static final String API_URL_BASE = "/modalidades/CICLO-TOYOTA/marcas/002?tipo-pessoa=PESSOA-FISICA";
    private static final String API_URL_BASE_CDC = "/modalidades/CDC/marcas/002?tipo-pessoa=PESSOA-FISICA";

    private MotorTaxaPlanoService motorTaxaPlanoService;
    private List<Plano> planos;
    private List<Plano> todosPlanosCDC;
    private List<Retorno> retornos;
    private Retorno retorno0;
    private Retorno retorno1;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private ErrorParser errorParser;

    @MockBean
    private FatorSimulacao fatorSimulacao;
    @MockBean
    private ResponseEntity responseEntity;

    @Before
    public void setup() {
        motorTaxaPlanoService = new MotorTaxaPlanoServiceImpl(restTemplate, new ApiService(), "COORDENADOR",
                apiMotorTaxaUrl, errorParser, oboFilterExpressionStr, filterExpressionStr, filtroIdModalidadeEnabled, fatorSimulacao);

        planos = new ArrayList<>();
        planos.add(new Plano(MODALIDADE, "1", "1", "CICLO LXT>30 121", 2018, 9999, DATA_INICIAL_VIGENCIA,
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,
                TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "12", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        planos.add(new Plano(MODALIDADE, "2", "1", "CICLO LXT>40 121", 2018, 9999, DATA_INICIAL_VIGENCIA,
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,
                TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.OBRIGATORIO_BALAO,null,null,null));
        planos.add(new Plano(MODALIDADE, "3", "1", "CICLO LXT>50 121", 2018, 9999, DATA_INICIAL_VIGENCIA,
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,
                TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.OBRIGATORIO_BALAO,null,null,null));
        planos.add(new Plano(MODALIDADE, "4", "1", "CICLO LXT>50 121", 2018, 9999, DATA_INICIAL_VIGENCIA.plusDays(30),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,
                TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "12", "N", "MENSAL", 1, true, false,EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA,null,null,null));

        retornos = new ArrayList<>();
        retorno0 = new Retorno("0", "0", "179");
        retorno1 = new Retorno("1", "1", "178");
        retornos.add(retorno0);
        retornos.add(retorno1);

        PlanosResponse planosResponse = new PlanosResponse(new PlanosResponse.Response(planos));
        RetornoMotorTaxaResponse retornoResponse = new RetornoMotorTaxaResponse(retornos);

        String urlOk = apiMotorTaxaUrl.concat(API_URL_BASE +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&aplicacao=DIRECT");
        when(restTemplate.exchange(eq(urlOk), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponse, HttpStatus.OK));

        String urlNotFound = apiMotorTaxaUrl.concat(API_URL_BASE +
                "&situacao-veiculo=usado&venda-direta=false&subsidio=NAO&ano-modelo=2019&codigo-modelo=001265&regiao=CANOPUS&aplicacao=DIRECT");
        when(restTemplate.exchange(eq(urlNotFound), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        String urlInternalServerError = apiMotorTaxaUrl.concat(API_URL_BASE +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2019&codigo-modelo=001265&regiao=CANOPUS&aplicacao=DIRECT");
        when(restTemplate.exchange(eq(urlInternalServerError), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        when(restTemplate.exchange(eq(apiMotorTaxaUrl.concat("/plano/2711/codigos-simulacao")), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenReturn(new ResponseEntity<>(retornoResponse, HttpStatus.OK));
        when(restTemplate.exchange(eq(apiMotorTaxaUrl.concat("/plano/0000/codigos-simulacao")), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        when(restTemplate.exchange(eq(apiMotorTaxaUrl.concat("/plano/null/codigos-simulacao")), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        todosPlanosCDC = new ArrayList<>();
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "1", "1", "CDC >30 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "01", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "2", "1", "CDC >40 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "01", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "3", "1", "CDC >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "01", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "4", "1", "CDC >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA.plusDays(30),new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "01", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
		todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "5", "1", "CREDITO INTELIGENTE >30 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "013", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "6", "1", "CREDITO INTELIGENTE >40 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "013", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "7", "1", "CREDITO INTELIGENTE >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "13", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
		todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "8", "1", "CREDITO FACIL >30 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "18", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "9", "1", "CREDITO FACIL >40 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "018", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));
        todosPlanosCDC.add(new Plano(MODALIDADE_CDC, "10", "1", "CREDITO FACIL >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30, "018", "N", "MENSAL", 1, true, false, EnumControleBalao.PERMITE_BALAO,null,null,null));

        PlanosResponse planosResponseCDC = new PlanosResponse(new PlanosResponse.Response(todosPlanosCDC));

        String urlOkCDC = apiMotorTaxaUrl.concat(API_URL_BASE_CDC +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&aplicacao=DIRECT");
        when(restTemplate.exchange(eq(urlOkCDC), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponseCDC, HttpStatus.OK));

        //Altera o valor da variável do MotorTaxaMotorTaxaPlanoServiceImpl
		try {
			Field filtroIdModalidadeEnabled = motorTaxaPlanoService.getClass().getDeclaredField("filtroIdModalidadeEnabled");
			Boolean filtroIdModalidadeEn = true;
			filtroIdModalidadeEnabled.setAccessible(true);
			filtroIdModalidadeEnabled.set(motorTaxaPlanoService, filtroIdModalidadeEn);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    // Metodo só para não quebrar na execução - Aguardando api de motor Taxa estar no ar para habilitar metodos
   /* @Test
    public void deveRetornarTodosOsPlanos() {

      assertEquals(0L, 0L);
    }*/

/*
    @Test
    public void deveRetornarTodosOsPlanos() {
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM,
                        false, false, -1), CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2020,
                        null, true,null);
        //Rubens
        assertArrayEquals(planos.toArray(),
                motorTaxaPlanoService.getPlanos(filtrosPlano, null).toArray());
    } */

    private FiltrosPlano filtrosPlano() {
        return new FiltrosPlano(new ConfiguracaoPlano(br.com.bancotoyota.services.simulador.entities.Modalidade.CICLO_TOYOTA, br.com.bancotoyota.services.simulador.entities.TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1),"DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,null);
    }

    private FiltrosPlano filtrosPlanoCDC() {
        return new FiltrosPlano(new ConfiguracaoPlano(br.com.bancotoyota.services.simulador.entities.Modalidade.CDC, br.com.bancotoyota.services.simulador.entities.TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1),"DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,null);
    }

    private FiltrosPlano filtrosPlanoCreditoInteligente() {
    	List<String> idsModalidade = new ArrayList<>();
    	idsModalidade.add("13");
        return new FiltrosPlano(new ConfiguracaoPlano(br.com.bancotoyota.services.simulador.entities.Modalidade.CDC, br.com.bancotoyota.services.simulador.entities.TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1),"DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true, idsModalidade );
    }

    private FiltrosPlano filtrosPlanoCreditoFacil() {
    	List<String> idsModalidade = new ArrayList<>();
    	idsModalidade.add("18");
        return new FiltrosPlano(new ConfiguracaoPlano(br.com.bancotoyota.services.simulador.entities.Modalidade.CDC, br.com.bancotoyota.services.simulador.entities.TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1),"DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,idsModalidade);
    }

    @Test
    public void deveRetornarCicloToyota30Porcento() {
        mockRequestMotorTaxaPlanosCicloComFiltro("modalidade=CICLO-TOYOTA&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(30000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano());
        Assert.assertEquals("1", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarCicloToyota40Porcento() {
        mockRequestMotorTaxaPlanosCicloComFiltro("modalidade=CICLO-TOYOTA&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");

        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(40000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano());
        Assert.assertEquals("2", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarCicloToyota50Porcento() {
        mockRequestMotorTaxaPlanosCicloComFiltro("modalidade=CICLO-TOYOTA&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");

        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano());
        Assert.assertEquals("4", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarPlanoCDC50() {
        mockRequestMotorTaxaPlanosCDCComFiltro("modalidade=CDC&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");

        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCDC());
        Assert.assertEquals("4", planoCicloToyota.getCodigoPlano());
    }
    @Test
    public void deveRetornarPlanosCreditoInteligente50() {
        mockRequestMotorTaxaPlanosCDCComFiltro("modalidade=CDC&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");

        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCreditoInteligente());
        Assert.assertEquals("7", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarPlanosCreditoFacil50() {
        mockRequestMotorTaxaPlanosCDCComFiltro("modalidade=CDC&marca-id=002&tipo-pessoa=PESSOA-FISICA&zero-km=true&venda-direta=false&plano-subsidiado=false&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS&perfil=COORDENADOR&cnpj-origem-negocio=05868140232815&aplicacao=DIRECT");
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = motorTaxaPlanoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCreditoFacil());
        Assert.assertEquals("10", planoCicloToyota.getCodigoPlano());
    }

    private void mockRequestMotorTaxaPlanosCicloComFiltro(String filtros) {
        final LocalDate dataInicioVigencia = LocalDate.parse("2019-03-14");
        final Integer qtdeMinimaCarencia = 10;
        final Integer qtdeMaximaCarencia = 90;
        List<PlanoMotorTaxa> planos = new ArrayList<>();
        Taxa taxa = new Taxa();
        List<TaxasDetalhe> taxas = new ArrayList<>();
        TaxasDetalhe taxasDetalhe = new TaxasDetalhe();
        taxasDetalhe.setPercentualMinimoEntrada(TRINTA);
        taxasDetalhe.setPercentualMaximoEntrada(CEM);
        taxa.setTaxasDetalhe(taxas);

        DadosExternos dadosExternos = new DadosExternos();
        dadosExternos.setCodigoModalidade(Modalidade.CICLO_TOYOTA);
        dadosExternos.setIdModalidade("12");
        ParcelasIntermediarias parcelasIntermediarias = new ParcelasIntermediarias();
        parcelasIntermediarias.setPercentualMaximoBalao(CINQUENTA);
        parcelasIntermediarias.setPercentualMinimoBalao(BigDecimal.ZERO);
        parcelasIntermediarias.setPermiteBalao(true);

        List<TipoVeiculo> listaTiposVeiculo = new ArrayList<>();

        TipoVeiculo tipoVeiculo = new TipoVeiculo();
        tipoVeiculo.setAnoModeloFinal(9999);
        tipoVeiculo.setAnoModeloInicio(2018);
        listaTiposVeiculo.add(tipoVeiculo);
        PrimeiroVencimento primeiroVencimento = new PrimeiroVencimento();
        SubsidioMotorTaxa subsidioResponse = SubsidioMotorTaxa.builder()
                .tipoSubsidio(TipoSubsidio.FIXO)
                .build();

        planos.add( new PlanoMotorTaxa(new ObjectId(), 1, 1, 1, "APROVADO", "CICLO LXT>30 121", "CICLO LXT>30 121",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                1, null,false, TRINTA, CEM, null,null, dadosExternos, null,
                null, primeiroVencimento, parcelasIntermediarias,null,null));

        final ParcelasIntermediarias parcelasIntermediariasPlano2 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano2.setPercentualMaximoBalao(CINQUENTA);
        parcelasIntermediariasPlano2.setPercentualMinimoBalao(BigDecimal.ZERO);
        parcelasIntermediariasPlano2.setObrigaBalao(true);
        parcelasIntermediariasPlano2.setPermiteBalao(true);

        Taxa taxa2 = new Taxa();
        List<TaxasDetalhe> taxas2 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe2 = new TaxasDetalhe();
        taxasDetalhe2.setPercentualMinimoEntrada(QUARENTA);
        taxasDetalhe2.setPercentualMaximoEntrada(CEM);
        taxa2.setTaxasDetalhe(taxas2);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 2, 1, 1, "APROVADO", "CICLO LXT>40 121", "CICLO LXT>40 121",
                2, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa2, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                2, null,false, QUARENTA, CEM, null,null, dadosExternos, null,
                null, primeiroVencimento, parcelasIntermediariasPlano2,null,null));

        final ParcelasIntermediarias parcelasIntermediariasPlano3 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano3.setObrigaBalao(true);

        Taxa taxa3 = new Taxa();
        List<TaxasDetalhe> taxas3 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe3 = new TaxasDetalhe();
        taxasDetalhe3.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe3.setPercentualMaximoEntrada(CEM);
        taxa3.setTaxasDetalhe(taxas3);
        planos.add( new PlanoMotorTaxa(new ObjectId(), 3, 1, 1, "APROVADO", "CICLO LXT>50 121", "CICLO LXT>50 121",
                2, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa3, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                3, null,false, CINQUENTA, CEM, null,null, dadosExternos, null,
                null, primeiroVencimento, parcelasIntermediariasPlano3,null,null));

        final ParcelasIntermediarias parcelasIntermediariasPlano4 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano4.setBalaoUltima(true);
        parcelasIntermediariasPlano4.setPermiteBalao(true);
        Taxa taxa4 = new Taxa();
        List<TaxasDetalhe> taxas4 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe4 = new TaxasDetalhe();
        taxasDetalhe4.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe4.setPercentualMaximoEntrada(CEM);
        taxa4.setTaxasDetalhe(taxas4);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 4, 1, 1, "APROVADO", "CICLO LXT>50 121", "CICLO LXT>50 121",
                2, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia.plusMonths(1), null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                4, null,false, CINQUENTA, CEM, null,null, dadosExternos, null,
                null, primeiroVencimento, parcelasIntermediariasPlano4,null,null));

        PlanosMotorTaxaResponse planosResponse = new PlanosMotorTaxaResponse(planos);

        String urlOk = apiMotorTaxaUrl.concat("/planos/filtros?" + filtros);
        when(restTemplate.exchange(eq(urlOk), any(), any(), eq(PlanosMotorTaxaResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponse, HttpStatus.OK));
    }

    private void mockRequestMotorTaxaPlanosCDCComFiltro(String filtros) {
        final LocalDate dataInicioVigencia = LocalDate.parse("2019-03-14");
        final Integer qtdeMinimaCarencia = 10;
        final Integer qtdeMaximaCarencia = 90;
        List<PlanoMotorTaxa> planos = new ArrayList<>();

        DadosExternos dadosExternos = new DadosExternos();
        dadosExternos.setCodigoModalidade(Modalidade.CICLO_TOYOTA);
        dadosExternos.setIdModalidade("12");
        ParcelasIntermediarias parcelasIntermediarias = new ParcelasIntermediarias();
        parcelasIntermediarias.setPermiteBalao(true);
        List<TipoVeiculo> listaTiposVeiculo = new ArrayList<>();

        TipoVeiculo tipoVeiculo = new TipoVeiculo();
        tipoVeiculo.setAnoModeloFinal(9999);
        tipoVeiculo.setAnoModeloInicio(2018);
        listaTiposVeiculo.add(tipoVeiculo);
        PrimeiroVencimento primeiroVencimento = new PrimeiroVencimento();
        SubsidioMotorTaxa subsidioResponse = SubsidioMotorTaxa.builder()
                .tipoSubsidio(TipoSubsidio.FIXO)
                .build();

        DadosExternos dadosExternos1 = new DadosExternos();
        dadosExternos1.setCodigoModalidade(Modalidade.CDC);
        dadosExternos1.setIdModalidade("01");

        Taxa taxa1 = new Taxa();
        List<TaxasDetalhe> taxas1 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe1 = new TaxasDetalhe();
        taxasDetalhe1.setPercentualMinimoEntrada(TRINTA);
        taxasDetalhe1.setPercentualMaximoEntrada(CEM);
        taxa1.setTaxasDetalhe(taxas1);

        final ParcelasIntermediarias parcelasIntermediariasPlano1 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano1.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 1, 1, 1, "APROVADO", "CDC >30 ", "CDC >30 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa1, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                1, null,false, TRINTA, CEM, null,null, dadosExternos1, null,
                null, primeiroVencimento, parcelasIntermediariasPlano1,null,null));

//        new Plano(MODALIDADE_CDC, "2", "1", "CDC >40 ", 2018, 9999, DATA_INICIAL_VIGENCIA,
//                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10,
//                90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30,
//                "01", "N", "MENSAL", 1, true, EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos2 = new DadosExternos();
        dadosExternos2.setCodigoModalidade(Modalidade.CDC);
        dadosExternos2.setIdModalidade("01");

        Taxa taxa2 = new Taxa();
        List<TaxasDetalhe> taxas2 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe2 = new TaxasDetalhe();
        taxasDetalhe2.setPercentualMinimoEntrada(QUARENTA);
        taxasDetalhe2.setPercentualMaximoEntrada(CEM);
        taxa2.setTaxasDetalhe(taxas2);

        final ParcelasIntermediarias parcelasIntermediariasPlano2 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano2.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 2, 1, 1, "APROVADO", "CDC >40 ", "CDC >40 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa2, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                2, null,false, QUARENTA, CEM, null,null, dadosExternos2, null,
                null, primeiroVencimento, parcelasIntermediariasPlano2,null,null));


//        new Plano(MODALIDADE_CDC, "3", "1", "CDC >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA
//                ,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA,
//                10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO,
//                30, "01", "N", "MENSAL", 1, true, EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos3 = new DadosExternos();
        dadosExternos3.setCodigoModalidade(Modalidade.CDC);
        dadosExternos3.setIdModalidade("01");

        Taxa taxa3 = new Taxa();
        List<TaxasDetalhe> taxas3 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe3 = new TaxasDetalhe();
        taxasDetalhe3.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe3.setPercentualMaximoEntrada(CEM);
        taxa3.setTaxasDetalhe(taxas3);

        final ParcelasIntermediarias parcelasIntermediariasPlano3 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano3.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 3, 1, 1, "APROVADO", "CDC >50 ", "CDC >50 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa3, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                3, null,false, CINQUENTA, CEM, null,null, dadosExternos3, null,
                null, primeiroVencimento, parcelasIntermediariasPlano3,null,null));

//        new Plano(MODALIDADE_CDC, "4", "1", "CDC >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA.plusDays(30),
//                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA,
//                10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30,
//                "01","N", "MENSAL", 1, true, EnumControleBalao.PERMITE_BALAO));
        DadosExternos dadosExternos4 = new DadosExternos();
        dadosExternos4.setCodigoModalidade(Modalidade.CDC);
        dadosExternos4.setIdModalidade("01");

        Taxa taxa4 = new Taxa();
        List<TaxasDetalhe> taxas4 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe4 = new TaxasDetalhe();
        taxasDetalhe4.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe4.setPercentualMaximoEntrada(CEM);
        taxa4.setTaxasDetalhe(taxas4);

        final ParcelasIntermediarias parcelasIntermediariasPlano4 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano4.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 4, 1, 1, "APROVADO", "CDC >50 ", "CDC >50 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia.plusMonths(1), null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa4, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                4, null,false, CINQUENTA, CEM, null,null, dadosExternos4, null,
                null, primeiroVencimento, parcelasIntermediariasPlano4,null,null));

//        new Plano(MODALIDADE_CDC, "5", "1", "CREDITO INTELIGENTE >30 ", 2018, 9999,
//                DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"),
//                CINQUENTA, 10, 90,TIPO_PLANO, false, null, false,
//                INDICADOR_FAIXA_ANO, 30, "013", "N", "MENSAL", 1, true,
//                EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos5 = new DadosExternos();
        dadosExternos5.setCodigoModalidade(Modalidade.CDC);
        dadosExternos5.setIdModalidade("013");

        Taxa taxa5 = new Taxa();
        List<TaxasDetalhe> taxas5 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe5 = new TaxasDetalhe();
        taxasDetalhe5.setPercentualMinimoEntrada(TRINTA);
        taxasDetalhe5.setPercentualMaximoEntrada(CEM);
        taxa5.setTaxasDetalhe(taxas5);

        final ParcelasIntermediarias parcelasIntermediariasPlano5 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano5.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 5, 1, 1, "APROVADO", "CREDITO INTELIGENTE >30 ", "CREDITO INTELIGENTE >30 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa5, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                5, null,false, TRINTA, CEM, null,null, dadosExternos5, null,
                null, primeiroVencimento, parcelasIntermediariasPlano5,null,null));
//        new Plano(MODALIDADE_CDC, "6", "1", "CREDITO INTELIGENTE >40 ", 2018, 9999,
//                DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"),
//                CINQUENTA, 10, 90,TIPO_PLANO, false, null, false,
//                INDICADOR_FAIXA_ANO, 30, "013", "N", "MENSAL", 1, true,
//                EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos6 = new DadosExternos();
        dadosExternos6.setCodigoModalidade(Modalidade.CDC);
        dadosExternos6.setIdModalidade("013");

        Taxa taxa6 = new Taxa();
        List<TaxasDetalhe> taxas6 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe6 = new TaxasDetalhe();
        taxasDetalhe6.setPercentualMinimoEntrada(QUARENTA);
        taxasDetalhe6.setPercentualMaximoEntrada(CEM);
        taxa6.setTaxasDetalhe(taxas6);

        final ParcelasIntermediarias parcelasIntermediariasPlano6 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano6.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 6, 1, 1, "APROVADO", "CREDITO INTELIGENTE >40 ", "CREDITO INTELIGENTE >40 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa6, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                6, null,false, QUARENTA, CEM, null,null, dadosExternos6, null,
                null, primeiroVencimento, parcelasIntermediariasPlano6,null,null));
//        new Plano(MODALIDADE_CDC, "7", "1", "CREDITO INTELIGENTE >50 ", 2018, 9999,
//                DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM,
//                new BigDecimal("0.00"), CINQUENTA, 10, 90,TIPO_PLANO, false, null,
//                false, INDICADOR_FAIXA_ANO, 30, "13", "N", "MENSAL", 1,
//                true, EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos7 = new DadosExternos();
        dadosExternos7.setCodigoModalidade(Modalidade.CDC);
        dadosExternos7.setIdModalidade("013");

        Taxa taxa7 = new Taxa();
        List<TaxasDetalhe> taxas7 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe7 = new TaxasDetalhe();
        taxasDetalhe7.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe7.setPercentualMaximoEntrada(CEM);
        taxa7.setTaxasDetalhe(taxas7);

        final ParcelasIntermediarias parcelasIntermediariasPlano7 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano7.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 7, 1, 1, "APROVADO", "CREDITO INTELIGENTE >50 ", "CREDITO INTELIGENTE >50 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa7, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                7, null,false, CINQUENTA, CEM, null,null, dadosExternos7, null,
                null, primeiroVencimento, parcelasIntermediariasPlano7,null,null));

//        new Plano(MODALIDADE_CDC, "8", "1", "CREDITO FACIL >30 ", 2018, 9999, DATA_INICIAL_VIGENCIA,
//                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), TRINTA, CEM, new BigDecimal("0.00"), CINQUENTA,
//                10, 90,TIPO_PLANO, false, null, false,
//                INDICADOR_FAIXA_ANO, 30, "18", "N", "MENSAL", 1, true,
//                EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos8 = new DadosExternos();
        dadosExternos8.setCodigoModalidade(Modalidade.CDC);
        dadosExternos8.setIdModalidade("018");

        Taxa taxa8 = new Taxa();
        List<TaxasDetalhe> taxas8 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe8 = new TaxasDetalhe();
        taxasDetalhe8.setPercentualMinimoEntrada(TRINTA);
        taxasDetalhe8.setPercentualMaximoEntrada(CEM);
        taxa8.setTaxasDetalhe(taxas8);

        final ParcelasIntermediarias parcelasIntermediariasPlano8 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano8.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 8, 1, 1, "APROVADO", "CREDITO FACIL >30 ", "CREDITO FACIL >30 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa8, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                8, null,false, TRINTA, CEM, null,null, dadosExternos8, null,
                null, primeiroVencimento, parcelasIntermediariasPlano8,null,null));


//        new Plano(MODALIDADE_CDC, "9", "1", "CREDITO FACIL >40 ", 2018, 9999,
//                DATA_INICIAL_VIGENCIA,new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), QUARENTA, CEM, new BigDecimal("0.00"),
//                CINQUENTA, 10, 90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO,
//                30, "018", "N", "MENSAL", 1, true, EnumControleBalao.PERMITE_BALAO));
        DadosExternos dadosExternos9 = new DadosExternos();
        dadosExternos9.setCodigoModalidade(Modalidade.CDC);
        dadosExternos9.setIdModalidade("018");

        Taxa taxa9 = new Taxa();
        List<TaxasDetalhe> taxas9 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe9 = new TaxasDetalhe();
        taxasDetalhe9.setPercentualMinimoEntrada(QUARENTA);
        taxasDetalhe9.setPercentualMaximoEntrada(CEM);
        taxa9.setTaxasDetalhe(taxas9);

        final ParcelasIntermediarias parcelasIntermediariasPlano9 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano9.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 9, 1, 1, "APROVADO", "CREDITO FACIL >40 ", "CREDITO FACIL >40 ",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa9, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                9, null,false, QUARENTA, CEM, null,null, dadosExternos9, null,
                null, primeiroVencimento, parcelasIntermediariasPlano9,null,null));

//        new Plano(MODALIDADE_CDC, "10", "1", "CREDITO FACIL >50 ", 2018, 9999, DATA_INICIAL_VIGENCIA,
//                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), CINQUENTA, CEM, new BigDecimal("0.00"), CINQUENTA, 10,
//                90,TIPO_PLANO, false, null, false, INDICADOR_FAIXA_ANO, 30,
//                "018", "N", "MENSAL", 1, true, EnumControleBalao.PERMITE_BALAO));

        DadosExternos dadosExternos10 = new DadosExternos();
        dadosExternos10.setCodigoModalidade(Modalidade.CDC);
        dadosExternos10.setIdModalidade("018");

        Taxa taxa10 = new Taxa();
        List<TaxasDetalhe> taxas10 = new ArrayList<>();
        TaxasDetalhe taxasDetalhe10 = new TaxasDetalhe();
        taxasDetalhe10.setPercentualMinimoEntrada(CINQUENTA);
        taxasDetalhe10.setPercentualMaximoEntrada(CEM);
        taxa10.setTaxasDetalhe(taxas10);

        final ParcelasIntermediarias parcelasIntermediariasPlano10 = new ParcelasIntermediarias();
        parcelasIntermediariasPlano10.setPermiteBalao(true);

        planos.add( new PlanoMotorTaxa(new ObjectId(), 10, 1, 1, "APROVADO", "CREDITO FACIL >50", "CREDITO FACIL >50",
                1, TipoPlano.NORMAL, TipoPessoa.PESSOA_FISICA, dataInicioVigencia, null, 30, qtdeMinimaCarencia,
                qtdeMaximaCarencia, TipoFiltro.TODOS, null,  taxa10, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null,
                TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS, null, TipoFiltro.TODOS,
                null, TipoFiltro.TODOS, listaTiposVeiculo, TipoFiltro.TODOS,null, false,subsidioResponse,
                10, null,false, CINQUENTA, CEM, null,null, dadosExternos10, null,
                null, primeiroVencimento, parcelasIntermediariasPlano10,null,null));

        PlanosMotorTaxaResponse planosResponse = new PlanosMotorTaxaResponse(planos);

        String urlOk = apiMotorTaxaUrl.concat("/planos/filtros?" + filtros);
        when(restTemplate.exchange(eq(urlOk), any(), any(), eq(PlanosMotorTaxaResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponse, HttpStatus.OK));
    }

    /*



    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNehumPlanoEncontrado() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(PlanosResponse.class)))
                .thenThrow(exception);
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.USADO,
                        false, false, -1), CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2019,
                null, true,null);
        motorTaxaPlanoService.getPlanos(filtrosPlano, null);
    } */

    /*
    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiPlanos() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(PlanosResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM,
                        false, false, -1), CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2019,
                null, true,null);
        motorTaxaPlanoService.getPlanos(filtrosPlano, null);
    } */

    @Test
    public void deveRetornarTodosOsRetornos() {
        assertArrayEquals(retornos.toArray(),
                motorTaxaPlanoService.getRetornos("2711", CNPJ_ORIGEM_NEGOCIO, null, null).toArray());
    }

    @Test
    public void deveRetornarDefault() {
        Retorno retrieved = motorTaxaPlanoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null);
        assertEquals(retorno1.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }
    
    
    @Test
    public void deveRetornarCodigoRetorno1() {
        Retorno retrieved = motorTaxaPlanoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "1");
        assertEquals(retorno1.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }    
    
    @Test
    public void deveRetornarCodigoRetorno0() {
        Retorno retrieved = motorTaxaPlanoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "0");
        assertEquals(retorno0.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }        
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumRetornoEncontradoGetRetorno1() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenThrow(exception);
        motorTaxaPlanoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "3");
    }    
// ainda não implementado
//    @Test
//    public void deveRetornarMenor() {
//        List<Retorno> retornos = new ArrayList<>();
//        retornos.add(new Retorno("2", "2", "180"));
//        retornos.add(retorno0);
//
//        RetornoResponse retornoResponse = new RetornoResponse(new RetornoResponse.Response(retornos));
//        when(restTemplate.exchange(eq(apiMotorTaxaUrl.concat("/2711/codigos-simulacao")), any(), any(), eq(RetornoResponse.class)))
//                .thenReturn(new ResponseEntity<>(retornoResponse, HttpStatus.OK));
//        Retorno retrieved = motorTaxaPlanoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, null);
//        assertEquals(retorno0.getCodigoRetorno(), retrieved.getCodigoRetorno());
//    }

    
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumRetornoEncontrado() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenThrow(exception);
        motorTaxaPlanoService.getRetornos("0000", CNPJ_ORIGEM_NEGOCIO, null, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiRetornos()  {
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoMotorTaxaResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        motorTaxaPlanoService.getRetornos(null, null, null, null);
    }

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosListaVazia() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        when(errorParser.parseList(any())).thenReturn(new ArrayList<>());
        motorTaxaPlanoService.findPrazos(1234, new ArrayList<>());
    }

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosListaNull() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        List<Erro> list = new ArrayList<>();
        list.add(new Erro());
        when(errorParser.parseList(any())).thenReturn(list);
        motorTaxaPlanoService.findPrazos(1234, null);
    }

    /*
    @Test
    public void findPrazosMesesNull() {
        List<Prazo> list = new ArrayList<>();
        ResponseEntity<List<Prazo>> response = new ResponseEntity<>(list, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenReturn(response);
        List<Prazo> prazos = motorTaxaPlanoService.findPrazos(1234, null);
        assertSame(list, prazos);
        prazos = motorTaxaPlanoService.findPrazos(1234, Collections.emptyList());
        assertSame(list, prazos);
    } */

    /*
    @Test
    public void findPrazosMeses() {
        List<Prazo> list = new ArrayList<>();
        Prazo p1 = new Prazo();
        p1.setNumeroDeParcelas(1);
        list.add(p1);

        Prazo p2 = new Prazo();
        p2.setNumeroDeParcelas(2);
        list.add(p2);

        ResponseEntity<List<Prazo>> response = new ResponseEntity<>(list, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenReturn(response);
        List<Prazo> prazos = motorTaxaPlanoService.findPrazos(1234, Arrays.asList(1));
        assertEquals(1, prazos.size());
    } */

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosNaoEncontradoComMeses() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        when(errorParser.parseList(any())).thenReturn(new ArrayList<>());
        motorTaxaPlanoService.findPrazos(1234, Arrays.asList(12));
    }

    @Test
    public void findSubsidioFaixasValorInputDesordenado() {
        // Lista com as faixas de valor na ordem correta, com a qual compararemos a lista obtida do findSubsidio.
        List<Subsidio.FaixaValor> faixasValorOrdemCorreta = new ArrayList<>();
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));

        // Objeto SubsidioMotorTaxa a ser convertido em um objeto Subsidio, que será usado no findSubsidio.
        SubsidioMotorTaxa subsidioFromMotorTaxa = new SubsidioMotorTaxa();
        subsidioFromMotorTaxa.setTipoSubsidio(TipoSubsidio.FIXO);
        subsidioFromMotorTaxa.setTaxaBanco(null);

        List<FaixasValorMotorTaxas> faixasValorOrdemAleatoria = new ArrayList<>();
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.MONTADORA, 1, new BigDecimal("18000.00"), new BigDecimal("9000.01")));
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.DEALER, 0, new BigDecimal("9000.00"), new BigDecimal("0.01")));
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.DEALER, 2, new BigDecimal("999999999999999.00"), new BigDecimal("18000.01")));
        subsidioFromMotorTaxa.setFaixasValorMotorTaxas(faixasValorOrdemAleatoria);

        SubsidioMotorTaxaResponse subsidioMotorTaxaResponse = new SubsidioMotorTaxaResponse();
        subsidioMotorTaxaResponse.setSubsidio(subsidioFromMotorTaxa);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<SubsidioMotorTaxaResponse>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidioMotorTaxaResponse);

        Subsidio subsidio = motorTaxaPlanoService.findSubsidio(1234);

        Boolean faixasOrdenadas = subsidio.getFaixasValor().stream()
                .allMatch(a -> faixasValorOrdemCorreta.stream()
                        .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial())
                        )
                );

        assertTrue(faixasOrdenadas);
    }

    @Test
    public void findSubsidioFaixasValorInputOrdenado() {
        // Lista com as faixas de valor na ordem correta, com a qual compararemos a lista obtida do findSubsidio.
        List<Subsidio.FaixaValor> faixasValorOrdemCorreta = new ArrayList<>();
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));

        // Objeto SubsidioMotorTaxa a ser convertido em um objeto Subsidio, que será usado no findSubsidio.
        SubsidioMotorTaxa subsidioFromMotorTaxa = new SubsidioMotorTaxa();
        subsidioFromMotorTaxa.setTipoSubsidio(TipoSubsidio.FIXO);
        subsidioFromMotorTaxa.setTaxaBanco(null);

        List<FaixasValorMotorTaxas> faixasValorOrdemAleatoria = new ArrayList<>();
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.DEALER, 0, new BigDecimal("9000.00"), new BigDecimal("0.01")));
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.MONTADORA, 1, new BigDecimal("18000.00"), new BigDecimal("9000.01")));
        faixasValorOrdemAleatoria.add(new FaixasValorMotorTaxas(ResponsabilidadeMotorTaxa.DEALER, 2, new BigDecimal("999999999999999.00"), new BigDecimal("18000.01")));
        subsidioFromMotorTaxa.setFaixasValorMotorTaxas(faixasValorOrdemAleatoria);

        SubsidioMotorTaxaResponse subsidioMotorTaxaResponse = new SubsidioMotorTaxaResponse();
        subsidioMotorTaxaResponse.setSubsidio(subsidioFromMotorTaxa);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<SubsidioMotorTaxaResponse>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidioMotorTaxaResponse);

        Subsidio subsidio = motorTaxaPlanoService.findSubsidio(1234);

        Boolean faixasOrdenadas = subsidio.getFaixasValor().stream()
                .allMatch(a -> faixasValorOrdemCorreta.stream()
                        .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial())
                        )
                );

        assertTrue(faixasOrdenadas);
    }

    @Test
    public void findSubsidioFaixasValorInputVazio() {
        // Lista com as faixas de valor na ordem correta, com a qual compararemos a lista obtida do findSubsidio.
        List<Subsidio.FaixaValor> faixasValorOrdemCorreta = new ArrayList<>();
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));

        // Objeto SubsidioMotorTaxa a ser convertido em um objeto Subsidio, que será usado no findSubsidio.
        SubsidioMotorTaxa subsidioFromMotorTaxa = new SubsidioMotorTaxa();
        subsidioFromMotorTaxa.setTipoSubsidio(TipoSubsidio.FIXO);
        subsidioFromMotorTaxa.setTaxaBanco(null);

        SubsidioMotorTaxaResponse subsidioMotorTaxaResponse = new SubsidioMotorTaxaResponse();
        subsidioMotorTaxaResponse.setSubsidio(subsidioFromMotorTaxa);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<SubsidioMotorTaxaResponse>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidioMotorTaxaResponse);

        Subsidio subsidio = motorTaxaPlanoService.findSubsidio(1234);

        Boolean faixasOrdenadas = false;
        if (subsidio != null && subsidio.getFaixasValor() != null && !subsidio.getFaixasValor().isEmpty()) {
            faixasOrdenadas = subsidio.getFaixasValor().stream()
                    .allMatch(a -> faixasValorOrdemCorreta.stream()
                            .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial())
                            )
                    );
        }

        assertFalse(faixasOrdenadas);
    }
    /*
    @Test
    public void findSubsidio() {
        Subsidio subsidio = new Subsidio();
        ResponseEntity<Subsidio> response = new ResponseEntity<>(subsidio, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<Subsidio>)any()))
                .thenReturn(response);
        Subsidio sub = motorTaxaPlanoService.findSubsidio(1234);
        assertSame(subsidio, sub);
    } */

    /*
    @Test
    public void getPlanoById() {
        PlanoResponse planoResponse = new PlanoResponse();
        Plano p = new Plano();
        planoResponse.setPlano(p);
        ResponseEntity<PlanoResponse> response = new ResponseEntity<>(planoResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<PlanoResponse>)any()))
                .thenReturn(response);
        Plano plano = motorTaxaPlanoService.convertPlanoFromMotorTaxa(motorTaxaPlanoService.getPlanoPorId(1234));
        assertSame(p, plano);
    }
    */
}