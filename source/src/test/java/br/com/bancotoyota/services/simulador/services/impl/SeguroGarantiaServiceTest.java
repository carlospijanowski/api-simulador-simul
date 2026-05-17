package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.SeguroGarantiaService;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class SeguroGarantiaServiceTest {

    @Value("${api.seguro.garantia.url}")
    private String apiSeguroGarantiaUrl;
    private SeguroGarantiaRequest seguroGarantiaRequest = new SeguroGarantiaRequest();

    private SeguroGarantiaService seguroGarantiaService;

    @MockBean
    private TokenService tokenService;
    
    private AccessToken token;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        seguroGarantiaRequest.setPossuiGarantia(true);
        seguroGarantiaRequest.setMarca("TOYOTA");
        seguroGarantiaRequest.setAnoFabricacao(2022);
        seguroGarantiaRequest.setDataInicioGarantia(LocalDate.now());
        seguroGarantiaRequest.setQuilometragem("23000");
        seguroGarantiaService = new SeguroGarantiaServiceImpl(restTemplate, new ApiService(), apiSeguroGarantiaUrl, tokenService);
        ComboSeguroGarantiaResponse comboSeguroGarantiaResponse = mapper.readValue(
                IOUtils.toString(getClass().getClassLoader().getResourceAsStream("seguro-garantia/garantia.json"), "UTF-8"), ComboSeguroGarantiaResponse.class);


        when(restTemplate.exchange(eq(apiSeguroGarantiaUrl.concat("/elegibilidades/")),
                any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenReturn(new ResponseEntity<>(comboSeguroGarantiaResponse, HttpStatus.OK));

        token = new AccessToken();
        token.setToken("Token");
        when(tokenService.generateAccessToken()).thenReturn(token);
        
    }

    @Test
    public void deveRetornarItens() {
        assertEquals(4, seguroGarantiaService.getValor("18919846081", seguroGarantiaRequest).getCombo().getItens().size());
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroApiGarantiaEstendiaMecanica() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        seguroGarantiaService.getValor(null, seguroGarantiaRequest);
    }

    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiGarantiaEstendiaMecanicaIndisponivel() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.exchange(anyString(), any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenThrow(ex);
        seguroGarantiaService.getValor(null, seguroGarantiaRequest);
    }

    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiGarantiaEstendiaMecanicaIndisponivelComInternalServerError() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(anyString(), any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenThrow(ex);
        seguroGarantiaService.getValor(null, seguroGarantiaRequest);
    }

    @Test(expected = ServicoForaException.class)
    public void deveRetornarServicoForaApiGarantiaEstendiaMecanicaTimeout() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.GATEWAY_TIMEOUT);
        when(restTemplate.exchange(anyString(), any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenThrow(ex);
        seguroGarantiaService.getValor(null, seguroGarantiaRequest);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarThirdPartyException() {
    	HttpServerErrorException ex = new HttpServerErrorException(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenThrow(ex);
        seguroGarantiaService.getValor(null, seguroGarantiaRequest);
    }


}