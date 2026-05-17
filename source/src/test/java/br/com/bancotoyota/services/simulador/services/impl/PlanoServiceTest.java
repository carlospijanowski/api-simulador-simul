package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.services.simulador.utils.ErrorParser;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class PlanoServiceTest {

    @Value("${api.planos.url}")
    private String apiPlanosUrl;

    @Value("${planos.obo.filter}")
    private String oboFilterExpressionStr;

    @Value("${planos.filter}")
    private String filterExpressionStr;

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

    private PlanoService planoService;
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
    private ResponseEntity responseEntity;

    @Before
    public void setup() {
        planoService = new PlanoServiceImpl(restTemplate, new ApiService(), "COORDENADOR",
                apiPlanosUrl, errorParser, oboFilterExpressionStr, filterExpressionStr);

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
        RetornoResponse retornoResponse = new RetornoResponse(new RetornoResponse.Response(retornos));

        String urlOk = apiPlanosUrl.concat(API_URL_BASE +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS");
        when(restTemplate.exchange(eq(urlOk), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponse, HttpStatus.OK));

        String urlNotFound = apiPlanosUrl.concat(API_URL_BASE +
                "&situacao-veiculo=usado&venda-direta=false&subsidio=NAO&ano-modelo=2019&codigo-modelo=001265&regiao=CANOPUS");
        when(restTemplate.exchange(eq(urlNotFound), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        String urlInternalServerError = apiPlanosUrl.concat(API_URL_BASE +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2019&codigo-modelo=001265&regiao=CANOPUS");
        when(restTemplate.exchange(eq(urlInternalServerError), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        when(restTemplate.exchange(eq(apiPlanosUrl.concat("/2711/codigos-simulacao")), any(), any(), eq(RetornoResponse.class)))
                .thenReturn(new ResponseEntity<>(retornoResponse, HttpStatus.OK));
        when(restTemplate.exchange(eq(apiPlanosUrl.concat("/0000/codigos-simulacao")), any(), any(), eq(RetornoResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        when(restTemplate.exchange(eq(apiPlanosUrl.concat("/null/codigos-simulacao")), any(), any(), eq(RetornoResponse.class)))
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
        
        String urlOkCDC = apiPlanosUrl.concat(API_URL_BASE_CDC +
                "&situacao-veiculo=0km&venda-direta=false&subsidio=NAO&ano-modelo=2020&codigo-modelo=001265&regiao=CANOPUS");
        when(restTemplate.exchange(eq(urlOkCDC), any(), any(), eq(PlanosResponse.class)))
                .thenReturn(new ResponseEntity<>(planosResponseCDC, HttpStatus.OK));
        
        //Altera o valor da variável do planoServiceImpl
		try {
			Field filtroIdModalidadeEnabled = planoService.getClass().getDeclaredField("filtroIdModalidadeEnabled");
			Boolean filtroIdModalidadeEn = true;
			filtroIdModalidadeEnabled.setAccessible(true);
			filtroIdModalidadeEnabled.set(planoService, filtroIdModalidadeEn);			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    @Test
    public void deveRetornarTodosOsPlanos() {
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM,
                        false, false, -1), "DIRECT", CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2020,
                        null, true,null);
        assertArrayEquals(planos.toArray(),
                planoService.getPlanos(filtrosPlano, null).toArray());
    }

    private FiltrosPlano filtrosPlano() {
        return new FiltrosPlano(new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1), "DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,null);
    }
    
    private FiltrosPlano filtrosPlanoCDC() {
        return new FiltrosPlano(new ConfiguracaoPlano(Modalidade.CDC, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1), "DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,null);
    }
    
    private FiltrosPlano filtrosPlanoCreditoInteligente() {
    	List<String> idsModalidade = new ArrayList<>();
    	idsModalidade.add("13");
        return new FiltrosPlano(new ConfiguracaoPlano(Modalidade.CDC, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1), "DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true, idsModalidade );
    }   
    
    private FiltrosPlano filtrosPlanoCreditoFacil() {
    	List<String> idsModalidade = new ArrayList<>();
    	idsModalidade.add("18");
        return new FiltrosPlano(new ConfiguracaoPlano(Modalidade.CDC, TipoPessoa.PESSOA_FISICA,
                SituacaoVeiculo.ZERO_KM, Boolean.FALSE, Boolean.FALSE, -1), "DIRECT",
                CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA,
                MODELO, 2020, null, true,idsModalidade);
    }       

    @Test
    public void deveRetornarCicloToyota30Porcento() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(30000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano(), null);
        Assert.assertEquals("1", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarCicloToyota40Porcento() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(40000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano(), null);
        Assert.assertEquals("2", planoCicloToyota.getCodigoPlano());
    }

    @Test
    public void deveRetornarCicloToyota50Porcento() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlano(), null);
        Assert.assertEquals("4", planoCicloToyota.getCodigoPlano());
    }
    
    @Test
    public void deveRetornarPlanoCDC50() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCDC(), null);
        Assert.assertEquals("4", planoCicloToyota.getCodigoPlano());
    }
    
    @Test
    public void deveRetornarPlanosCreditoInteligente50() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCreditoInteligente(), null);
        Assert.assertEquals("7", planoCicloToyota.getCodigoPlano());
    }
    
    @Test
    public void deveRetornarPlanosCreditoFacil50() {
        BigDecimal valorBem = new BigDecimal(100000);
        BigDecimal valorEntrada = new BigDecimal(50000);
        Plano planoCicloToyota = planoService.getPlanoCicloToyota(valorBem, valorEntrada, filtrosPlanoCreditoFacil(), null);
        Assert.assertEquals("10", planoCicloToyota.getCodigoPlano());
    }    

    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNehumPlanoEncontrado() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(PlanosResponse.class)))
                .thenThrow(exception);
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.USADO,
                        false, false, -1), "DIRECT",CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2019,
                null, true,null);
        planoService.getPlanos(filtrosPlano, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiPlanos() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(PlanosResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        FiltrosPlano filtrosPlano = new FiltrosPlano(
                new ConfiguracaoPlano(Modalidade.CICLO_TOYOTA, TipoPessoa.PESSOA_FISICA, SituacaoVeiculo.ZERO_KM,
                        false, false, -1), "DIRECT",CNPJ_ORIGEM_NEGOCIO, REGIAO, MARCA, MODELO, 2019,
                null, true,null);
        planoService.getPlanos(filtrosPlano, null);
    }

    @Test
    public void deveRetornarTodosOsRetornos() {
        assertArrayEquals(retornos.toArray(),
                planoService.getRetornos("2711", CNPJ_ORIGEM_NEGOCIO, null, null).toArray());
    }

    @Test
    public void deveRetornarDefault() {
        Retorno retrieved = planoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, null);
        assertEquals(retorno1.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }
    
    
    @Test
    public void deveRetornarCodigoRetorno1() {
        Retorno retrieved = planoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "1",null);
        assertEquals(retorno1.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }    
    
    @Test
    public void deveRetornarCodigoRetorno0() {
        Retorno retrieved = planoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "0",null);
        assertEquals(retorno0.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }        
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumRetornoEncontradoGetRetorno1() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoResponse.class)))
                .thenThrow(exception);
        planoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, "3",null);
    }    

    @Test
    public void deveRetornarMenor() {
        List<Retorno> retornos = new ArrayList<>();
        retornos.add(new Retorno("2", "2", "180"));
        retornos.add(retorno0);

        RetornoResponse retornoResponse = new RetornoResponse(new RetornoResponse.Response(retornos));
        when(restTemplate.exchange(eq(apiPlanosUrl.concat("/2711/codigos-simulacao")), any(), any(), eq(RetornoResponse.class)))
                .thenReturn(new ResponseEntity<>(retornoResponse, HttpStatus.OK));
        Retorno retrieved = planoService.getRetorno("2711", CNPJ_ORIGEM_NEGOCIO, null, null);
        assertEquals(retorno0.getCodigoRetorno(), retrieved.getCodigoRetorno());
    }

    
    
    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumRetornoEncontrado() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoResponse.class)))
                .thenThrow(exception);
        planoService.getRetornos("0000", CNPJ_ORIGEM_NEGOCIO, null, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiRetornos()  {
        when(restTemplate.exchange(anyString(), any(), any(), eq(RetornoResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        planoService.getRetornos(null, null, null, null);
    }

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosListaVazia() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        when(errorParser.parseList(any())).thenReturn(new ArrayList<>());
        planoService.findPrazos(1234, new ArrayList<>());
    }

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosListaNull() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        List<Erro> list = new ArrayList<>();
        list.add(new Erro());
        when(errorParser.parseList(any())).thenReturn(list);
        planoService.findPrazos(1234, null);
    }

    @Test
    public void findPrazosMesesNull() {
        List<Prazo> list = new ArrayList<>();
        ResponseEntity<List<Prazo>> response = new ResponseEntity<>(list, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenReturn(response);
        List<Prazo> prazos = planoService.findPrazos(1234, null);
        assertSame(list, prazos);
        prazos = planoService.findPrazos(1234, Collections.emptyList());
        assertSame(list, prazos);
    }

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
        List<Prazo> prazos = planoService.findPrazos(1234, Arrays.asList(1));
        assertEquals(1, prazos.size());
    }

    @Test(expected = EntityNotFoundException.class)
    public void findPrazosNaoEncontradoComMeses() {
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<List<Prazo>>)any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "not found", null, null, Charset.forName("UTF-8")));
        when(errorParser.parseList(any())).thenReturn(new ArrayList<>());
        planoService.findPrazos(1234, Arrays.asList(12));
    }

    @Test
    public void findSubsidio() {
        Subsidio subsidio = new Subsidio();
        ResponseEntity<Subsidio> response = new ResponseEntity<>(subsidio, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<Subsidio>)any()))
                .thenReturn(response);
        Subsidio sub = planoService.findSubsidio(1234);
        assertSame(subsidio, sub);
    }

    @Test
    public void findSubsidioFaixasValorInputDesordenado() {
        // Lista com as faixas de valor na ordem correta, com a qual compararemos a lista obtida do findSubsidio.
        List<Subsidio.FaixaValor> faixasValorOrdemCorreta = new ArrayList<>();
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemCorreta.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));

        // Subsidio com as faixas a serem ordenadas, e que retorna do mock.
        Subsidio subsidio = new Subsidio();
        List<Subsidio.FaixaValor> faixasValorOrdemAleatoria = new ArrayList<>();
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));
        subsidio.setFaixasValor(faixasValorOrdemAleatoria);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<Subsidio>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidio);

        Subsidio subsidioFaixasOrdenadas = planoService.findSubsidio(1234);

        Boolean faixasOrdenadas = subsidioFaixasOrdenadas.getFaixasValor().stream()
                        .allMatch(a -> faixasValorOrdemCorreta.stream()
                                .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial()))

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

        // Subsidio com as faixas a serem ordenadas, e que retorna do mock.
        Subsidio subsidio = new Subsidio();
        List<Subsidio.FaixaValor> faixasValorOrdemAleatoria = new ArrayList<>();
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal("9000.00")));
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("9000.01"), new BigDecimal("18000.00")));
        faixasValorOrdemAleatoria.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("18000.01"), new BigDecimal("999999999999999.00")));
        subsidio.setFaixasValor(faixasValorOrdemAleatoria);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<Subsidio>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidio);

        Subsidio subsidioFaixasOrdenadas = planoService.findSubsidio(1234);

        Boolean faixasOrdenadas = subsidioFaixasOrdenadas.getFaixasValor().stream()
                .allMatch(a -> faixasValorOrdemCorreta.stream()
                        .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial()))

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

        // Subsidio com as faixas a serem ordenadas, e que retorna do mock.
        Subsidio subsidio = new Subsidio();
        List<Subsidio.FaixaValor> faixasValorOrdemAleatoria = new ArrayList<>();
        subsidio.setFaixasValor(faixasValorOrdemAleatoria);

        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<Subsidio>)any())).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(subsidio);

        Subsidio subsidioFaixasOrdenadas = planoService.findSubsidio(1234);

        Boolean faixasOrdenadas = false;
        if (subsidioFaixasOrdenadas != null && subsidioFaixasOrdenadas.getFaixasValor() != null && !subsidioFaixasOrdenadas.getFaixasValor().isEmpty()) {
            faixasOrdenadas = subsidioFaixasOrdenadas.getFaixasValor().stream()
                    .allMatch(a -> faixasValorOrdemCorreta.stream()
                            .anyMatch(b -> a.getValorInicial().equals(b.getValorInicial()))

                    );
        }

        assertFalse(faixasOrdenadas);
    }

    @Test
    public void getPlanoById() {
        PlanoResponse planoResponse = new PlanoResponse();
        Plano p = new Plano();
        planoResponse.setPlano(p);
        ResponseEntity<PlanoResponse> response = new ResponseEntity<>(planoResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), isNull(), (ParameterizedTypeReference<PlanoResponse>)any()))
                .thenReturn(response);
        Plano plano = planoService.getPlanoById(1234);
        assertSame(p, plano);
    }
}
