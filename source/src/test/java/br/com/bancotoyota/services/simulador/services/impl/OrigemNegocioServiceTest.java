package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocioResponse;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.OrigemNegocioService;
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

import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class OrigemNegocioServiceTest {

    private static final String CNPJ = "03215790000110";

    @Value("${api.origens-negocios.url}s")
    private String apiOrigensNegociosUrl;

    private OrigemNegocioService origemNegocioService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        origemNegocioService = new OrigemNegocioServiceImpl(restTemplate, new ApiService(), apiOrigensNegociosUrl);

        OrigemNegocio origemNegocio = new OrigemNegocio("811", CNPJ, "S Paulo", "SP", "A", CNPJ,
                "MASAHITO VELOSOQ LEVENTER", CNPJ, "SP CAPITAL", false, true, true, true, true, true);

        OrigemNegocioResponse origemNegocioResponse = new OrigemNegocioResponse(new OrigemNegocioResponse.Response(origemNegocio));
        ResponseEntity<OrigemNegocioResponse> response = new ResponseEntity<>(origemNegocioResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq(apiOrigensNegociosUrl.concat("/03215790000110")), any(), any(),
                eq(OrigemNegocioResponse.class))).thenReturn(response);
        when(restTemplate.exchange(eq(apiOrigensNegociosUrl.concat("/12345678000199")), any(), any(),
                eq(OrigemNegocioResponse.class))).thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        when(restTemplate.exchange(eq(apiOrigensNegociosUrl.concat("/")), any(), any(),
                eq(OrigemNegocioResponse.class))).thenReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
    }

    @Test
    public void deveRetornarUmaOrigemNegocio() {
        OrigemNegocio origemNegocio = origemNegocioService.getOrigemNegocio(CNPJ, null);
        assertNotNull(origemNegocio);
    }

    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroOrigemNegocioNaoEncontrada() {
        HttpClientErrorException exception = HttpClientErrorException.create(HttpStatus.NOT_FOUND, null,
                null, "[{\"message\":\"\"}]".getBytes(), Charset.defaultCharset());
        when(restTemplate.exchange(anyString(), any(), any(), eq(OrigemNegocioResponse.class)))
                .thenThrow(exception);
        origemNegocioService.getOrigemNegocio("", null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroAPIOrigensNegocios() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(OrigemNegocioResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        origemNegocioService.getOrigemNegocio("", null);
    }
}