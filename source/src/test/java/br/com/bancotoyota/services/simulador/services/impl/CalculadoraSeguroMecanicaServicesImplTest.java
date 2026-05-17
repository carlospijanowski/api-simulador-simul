package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.beans.SeguroMecanicaSelecionado;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.entities.ComboSeguroGarantiaResponse;
import br.com.bancotoyota.services.simulador.entities.ItemSeguroGarantia;
import br.com.bancotoyota.services.simulador.services.SeguroGarantiaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class CalculadoraSeguroMecanicaServicesImplTest {

    @InjectMocks
    private CalculadoraSeguroMecanicaServicesImpl calculadoraService;

    @Mock
    private SeguroGarantiaService seguroGarantiaService;

    private ParametrosSeguroMecanica parametros;
    private ComboSeguroGarantiaResponse comboResponse;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());  // Registra o módulo para lidar com tipos de data do Java 8

        String jsonString = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("seguro-garantia/garantia.json"), "UTF-8");

        comboResponse = mapper.readValue(jsonString, ComboSeguroGarantiaResponse.class);
        parametros = new ParametrosSeguroMecanica();
        parametros.setCnpjOrigemNegocio("12345678901234");
        parametros.setAnoFabricacao(2022);
        parametros.setPossuiSeguroMecanica(false);
        parametros.setCodigoFipe("1231");
        parametros.setAnoFabricacao(2022);
        parametros.setAnoModelo("2022");
        parametros.setPossuiSeguroMecanica(true);
        parametros.setMarca("Toyota");
        parametros.setQuilometragem("10000");

        ItemSeguroGarantia item = new ItemSeguroGarantia();
        item.setPrazoSeguro(12);
        item.setValorPremioTotal(BigDecimal.valueOf(1000));
        item.setId("123");


    }

    @Test
    public void testCalcular() throws IOException {

        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNotNull(response);
        assertNotNull(response.getCotacoes());
        assertNotNull(response.getSelecionado());
    }

    @Test
    public void deveRetornarNulo() throws IOException {

        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(new ComboSeguroGarantiaResponse());

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNull(response.getCotacoes());
        assertNull(response.getSelecionado());
    }

    @Test
    public void testOrdenarItensPorPrazoEValor() throws JsonProcessingException {

        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNotNull(response);
        assertEquals(new BigDecimal(2305.83).setScale(2, RoundingMode.HALF_EVEN), response.getCotacoes().get(0).getValorTotal());
        assertEquals(new BigDecimal(100).setScale(2, RoundingMode.HALF_EVEN), response.getCotacoes().get(1).getValorTotal());
    }

    @Test
    public void testOrdenarItensPorValor() throws JsonProcessingException {
        comboResponse.getCombo().getItens().get(2).setValorPremioTotal(BigDecimal.valueOf(400));
        comboResponse.getCombo().getItens().get(0).setPrazoSeguro(12);
        comboResponse.getCombo().getItens().get(1).setPrazoSeguro(12);
        comboResponse.getCombo().getItens().get(2).setPrazoSeguro(12);
        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNotNull(response);
        assertEquals(new BigDecimal(400).setScale(2, RoundingMode.HALF_EVEN), response.getCotacoes().get(0).getValorTotal());
    }

    @Test
    public void testOrdenarItensPorPrazo() throws JsonProcessingException {
        comboResponse.getCombo().getItens().get(2).setPrazoSeguro(2);
        comboResponse.getCombo().getItens().get(0).setValorPremioTotal(BigDecimal.valueOf(400));
        comboResponse.getCombo().getItens().get(1).setValorPremioTotal(BigDecimal.valueOf(400));
        comboResponse.getCombo().getItens().get(2).setValorPremioTotal(BigDecimal.valueOf(400));
        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNotNull(response);
        assertEquals(2, response.getCotacoes().get(0).getPrazo());
    }

    @Test
    public void testObterMenorValorPremioTotal() throws JsonProcessingException {
        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        BigDecimal menorValorEsperado = BigDecimal.valueOf(2305.83).setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(menorValorEsperado, response.getCotacoes().get(0).getValorTotal());
    }

    @Test
    public void testSelecionarSeguro() throws JsonProcessingException {

        parametros.setSeguroMecanicaSelecionado(new SeguroMecanicaSelecionado());
        parametros.getSeguroMecanicaSelecionado().setId("2");
        parametros.getSeguroMecanicaSelecionado().setPrazo(12);

        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNotNull(response.getSelecionado());
        assertEquals("2", response.getSelecionado().getId());
        assertEquals(12, response.getSelecionado().getPrazo());
    }

    @Test
    public void deveRetornaSeguroSelecionadoVazio() throws JsonProcessingException {

        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);

        assertNull(response.getSelecionado().getId());
        assertNull(response.getSelecionado().getValorTotal());
        assertNull(response.getSelecionado().getDescricao());
        assertNull(response.getSelecionado().getDataInicioSeguroMecanica());
        assertNull(response.getSelecionado().getValorParcela());
        assertNull(response.getSelecionado().getCnpjSeguradora());
        assertNull(response.getSelecionado().getPrazo());
    }

    @Test
    public void deveRetornarValorPremioTotalSemSubtrair() {
        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);
        assertEquals(new BigDecimal("2305.83"), response.getCotacoes().get(0).getValorTotal());
    }

    @Test
    public void deveRetornarValorPremioTotalSubtraido() {
        when(seguroGarantiaService.getValor(Mockito.anyString(), Mockito.any())).thenReturn(comboResponse);

        CalculoSeguroMecanicaResponse response = calculadoraService.calcular(parametros, 12);
        assertEquals(new BigDecimal("100.00"), response.getCotacoes().get(1).getValorTotal());
    }

}