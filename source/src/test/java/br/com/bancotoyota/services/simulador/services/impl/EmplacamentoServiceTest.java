package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.entities.Estado;
import br.com.bancotoyota.services.simulador.entities.EstadoResponse;
import br.com.bancotoyota.services.simulador.entities.UfEmplacamento;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.EmplacamentoService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class EmplacamentoServiceTest {

    @Value("${api.emplacamentos.url}")
    private String apiEmplcamentosUrl;
    private String apiEmplcamentosEstadosUrl;

    private EmplacamentoService emplacamentoService;

    @MockBean
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        emplacamentoService = new EmplacamentoServiceImpl(restTemplate, new ApiService(), apiEmplcamentosUrl);
        apiEmplcamentosEstadosUrl = apiEmplcamentosUrl.concat("/estados");

        List<Estado> estados = new ArrayList<>();
        estados.add(new Estado("SP", "Todas", BigDecimal.valueOf(116.09), "500", LocalDateTime.now()));
        estados.add(new Estado("RJ", "Todas", BigDecimal.valueOf(62.22), "500", LocalDateTime.now()));

        EstadoResponse estadoResponse = new EstadoResponse(new EstadoResponse.Response(estados));
        ResponseEntity<EstadoResponse> response = new ResponseEntity<>(estadoResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq(apiEmplcamentosEstadosUrl), any(), any(), eq(EstadoResponse.class)))
                .thenReturn(response);
    }

    @Test
    public void deveRetornarUmEstado() {
        Estado estado = emplacamentoService.getEstado(UfEmplacamento.SP, null);
        assertNotNull(estado);
    }

    @Test(expected = EntityNotFoundException.class)
    public void deveRetornarErroEstadoNaoEncontrado() {
        emplacamentoService.getEstado(UfEmplacamento.MG, null);
    }

    @Test(expected = ThirdPartyException.class)
    public void deveRetornarErroAPIEmplacamentos() {
        when(restTemplate.exchange(eq(apiEmplcamentosEstadosUrl), any(), any(), eq(EstadoResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);
        emplacamentoService.getEstado(UfEmplacamento.ES, null);
    }
}