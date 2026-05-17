package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.AccessToken;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.SeguroGarantiaService;
import br.com.bancotoyota.services.simulador.services.TokenService;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
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
public class SeguroMecanicaServiceTest {

    @Value("${api.seguro.garantia.url}")
    private String apiSeguroGarantiaUrl;
    private SeguroGarantiaRequest seguroGarantiaRequest = new SeguroGarantiaRequest();
    private ComboSeguroGarantiaResponse comboSeguroGarantiaResponse ;

    private SeguroGarantiaService seguroGarantiaService;

    @MockBean
    private TokenService tokenService;
    
    private AccessToken token;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        seguroGarantiaRequest.setAnoFabricacao(2022);
        seguroGarantiaRequest.setPossuiGarantia(true);
        seguroGarantiaRequest.setMarca("Toyota");
        seguroGarantiaRequest.setAnoModelo("2023");
        seguroGarantiaRequest.setCodigoFipe("002207127");
        seguroGarantiaRequest.setModelo("YARIS XL PLUS CON. SED. 1.5 FLEX 16V AUT");
        seguroGarantiaRequest.setQuilometragem("0");
        seguroGarantiaService = new SeguroGarantiaServiceImpl(restTemplate, new ApiService(), apiSeguroGarantiaUrl, tokenService);
        mapper.registerModule(new JavaTimeModule());


        comboSeguroGarantiaResponse = mapper.readValue(
                IOUtils.toString(getClass().getClassLoader().getResourceAsStream("seguro-garantia/garantia.json"), "UTF-8"), ComboSeguroGarantiaResponse.class);


        when(restTemplate.exchange(anyString(),
                any(), any(), eq(ComboSeguroGarantiaResponse.class)))
                .thenReturn(new ResponseEntity<>(comboSeguroGarantiaResponse, HttpStatus.OK));

        token = new AccessToken();
        token.setToken("Token");
        when(tokenService.generateDirectAccessToken()).thenReturn(token);
        
    }

    @Test
    public void deveRetornarItens() {
        String cnpj = "18919846081";

        when(restTemplate.exchange(
                eq(apiSeguroGarantiaUrl + "/elegibilidades/" + cnpj),
                any(),
                any(),
                eq(ComboSeguroGarantiaResponse.class))
        ).thenReturn(new ResponseEntity<>(comboSeguroGarantiaResponse, HttpStatus.OK));

        ComboSeguroGarantiaResponse response = seguroGarantiaService.getValor(cnpj, seguroGarantiaRequest);

        assertNotNull(response);
        assertEquals(3, response.getCombo().getItens().size());
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