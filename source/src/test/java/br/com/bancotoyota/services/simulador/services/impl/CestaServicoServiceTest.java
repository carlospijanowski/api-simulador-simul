package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.CestaServicoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class CestaServicoServiceTest {

    @Value("${api.cestas-servicos.url}")
    private String apiCestasServicosUrl;

    private CestaServicoService cestaServicoService;
    private List<CestaServico> cestasServicos;
    private List<CestaItem> cestaItems;
    private CestaServico cestaServicoMaisCara;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        cestaServicoService = new CestaServicoServiceImpl(restTemplate, new ApiService(), "COORDENADOR", apiCestasServicosUrl);

        cestasServicos = new ArrayList<>();
        cestaServicoMaisCara = new CestaServico(32, "Cesta 10 - Pessoa Fisica", new BigDecimal(1100), false, false, false, null);
        cestasServicos.add(new CestaServico(28, "Cesta 9 - Pessoa Fisica", new BigDecimal(650), false, false, false, null));
        cestasServicos.add(cestaServicoMaisCara);

        cestaItems = new ArrayList<>();
        cestaItems.add(new CestaItem(1, new BigDecimal(550), 1, "CONFECÇÃO DE CADASTRO - PF", true));
        cestaItems.add(new CestaItem(3, new BigDecimal(320), 1, "TARIFA DE ADITAMENTO", false));

        CestaServicoResponse cestaServicoResponse = new CestaServicoResponse(new CestaServicoResponse.Response(cestasServicos));
        DespesaResponse despesaResponse = new DespesaResponse(new DespesaResponse.Response(cestaItems));

        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/origem-negocio/02010865237172?modalidade=CDC&tipo-pessoa=PESSOA-FISICA")),
                any(), any(), eq(CestaServicoResponse.class))).thenReturn(new ResponseEntity<>(cestaServicoResponse, HttpStatus.OK));
        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/origem-negocio/03215790000110?modalidade=LEASING&tipo-pessoa=PESSOA-JURIDICA")),
                any(), any(), eq(CestaServicoResponse.class))).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/origem-negocio/null?modalidade=null&tipo-pessoa=null")),
                any(), any(), eq(CestaServicoResponse.class))).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/24")), any(), any(), eq(DespesaResponse.class)))
                .thenReturn(new ResponseEntity<>(despesaResponse, HttpStatus.OK));
        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/23")), any(), any(), eq(DespesaResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        when(restTemplate.exchange(eq(apiCestasServicosUrl.concat("/null")), any(), any(), eq(DespesaResponse.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void deveRetornarTodasAsCestasServicos() {
        assertArrayEquals(cestasServicos.toArray(),
                cestaServicoService.getCestasServicos("02010865237172", Modalidade.CDC, TipoPessoa.PESSOA_FISICA,
                        null,null).toArray());
    }

    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumaCestaServicoEncontrada() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "{\"message\":\"\"}".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(CestaServicoResponse.class)))
                .thenThrow(exception);
        cestaServicoService.getCestasServicos(null, null, null, null, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiCestasServicos() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(CestaServicoResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        cestaServicoService.getCestasServicos(null, null, null, null, null);
    }

    @Test
    public void deveRetornarCestaServicoMaisCara() {
        CestaServico retrieved = cestaServicoService.getCestaServicoMaisCara("02010865237172", Modalidade.CDC,
                TipoPessoa.PESSOA_FISICA, null, null);
        assertEquals(cestaServicoMaisCara.getCodigo(), retrieved.getCodigo());
    }

    @Test
    public void deveRetornarTodasAsDespesas() {
        assertArrayEquals(cestaItems.toArray(),
                cestaServicoService.getDespesas(24, null).toArray());
    }

    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroNenhumaDespesaEncontrada() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "{\"message\":\"\"}".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(DespesaResponse.class)))
                .thenThrow(exception);
        cestaServicoService.getDespesas(null, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiDespesas() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(DespesaResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        cestaServicoService.getDespesas(null, null);
    }
}
