package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.DadosSimulacao;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.ApiService;
import br.com.bancotoyota.services.simulador.services.CestaServicoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.ThirdPartyException;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.DadosSimulacaoPlanilha;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class CalculadoraServicesImplTest {



    private CalculadoraServicesImpl calculadoraServices;

    @Before
    public void setup() throws JsonProcessingException {
        calculadoraServices = new CalculadoraServicesImpl();
    }

    @Test
    public void deveRetornaValorSeguroAutoSomenteComSeguroFranquia() throws JsonProcessingException {
        String str = "{\n" + "  \"idSimulacao\" : \"FATOR_PARA_VALOR_NA_PARCELA\",\n"
                + "  \"dataSimulacao\" : [ 2019, 2, 18 ],\n" + "  \"dataPrimeiroVencimento\" : [ 2019, 6, 22 ],\n"
                + "  \"valorBem\" : 0,\n" + "  \"valorEntrada\" : 0,\n" + "  \"valorSeguroPrestamista\" : 0,\n" + "  \"vlrSeguroFranquia\" : 4500,\n"
                + "  \"custoPercentualDoSeguroPrestamista\" : null,\n" + "  \"valorRegistroContrato\" : 0,\n"
                + "  \"valorCestaServico\" : 0,\n" + "  \"valorTC\" : 0,\n" + "  \"valorTaxaSubsidio\" : null,\n"
                + "  \"codigoRetorno\" : \"1\",\n" + "  \"periodicidade\" : 1,\n"
                + "  \"tipoFinanciamento\" : \"CDC\",\n" + "  \"tipoCalculo\" : \"TAXA\",\n"
                + "  \"valorSubsidio\" : null,\n" + "  \"temIsencaoIOF\" : false,\n" + "  \"taxaIOFAA\" : 0.030000,\n"
                + "  \"taxaIOFAD\" : 0.003800,\n" + "  \"baseCalculoIOF\" : 365,\n" + "  \"parcelasBalao\" : [ ],\n"
                + "  \"prazo\" : {\n" + "    \"prazo\" : 12,\n" + "    \"taxa-mes\" : 1.308033,\n"
                + "    \"perc-min-balao\" : 20.00,\n" + "    \"perc-max-balao\" : 50.00,\n"
                + "    \"perc-min-entrada\" : 20,\n" + "    \"perc-max-entrada\" : 50\n" + "  },\n"
                + "  \"tipoPessoa\" : \"pessoa-fisica\",\n" + "  \"itens\" : null\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        DadosSimulacao dadosSimulacao = mapper.readValue(str, DadosSimulacao.class);
        DadosSimulacaoPlanilha result = calculadoraServices.getDadosSimulacaoPlanilha(dadosSimulacao);
        assertEquals(new BigDecimal(result.getValorSeguro()), dadosSimulacao.getVlrSeguroFranquia());
    }

    @Test
    public void deveRetornaValorSeguroAutoSomenteComSeguroPrestamista() throws JsonProcessingException {
        String str = "{\n" + "  \"idSimulacao\" : \"FATOR_PARA_VALOR_NA_PARCELA\",\n"
                + "  \"dataSimulacao\" : [ 2019, 2, 18 ],\n" + "  \"dataPrimeiroVencimento\" : [ 2019, 6, 22 ],\n"
                + "  \"valorBem\" : 0,\n" + "  \"valorEntrada\" : 0,\n" + "  \"valorSeguroPrestamista\" : 0,\n"
                + "  \"custoPercentualDoSeguroPrestamista\" : null,\n" + "  \"valorRegistroContrato\" : 0,\n"
                + "  \"valorCestaServico\" : 0,\n" + "  \"valorTC\" : 0,\n" + "  \"valorTaxaSubsidio\" : null,\n"
                + "  \"codigoRetorno\" : \"1\",\n" + "  \"periodicidade\" : 1,\n"
                + "  \"tipoFinanciamento\" : \"CDC\",\n" + "  \"tipoCalculo\" : \"TAXA\",\n"
                + "  \"valorSubsidio\" : null,\n" + "  \"temIsencaoIOF\" : false,\n" + "  \"taxaIOFAA\" : 0.030000,\n"
                + "  \"taxaIOFAD\" : 0.003800,\n" + "  \"baseCalculoIOF\" : 365,\n" + "  \"parcelasBalao\" : [ ],\n"
                + "  \"prazo\" : {\n" + "    \"prazo\" : 12,\n" + "    \"taxa-mes\" : 1.308033,\n"
                + "    \"perc-min-balao\" : 20.00,\n" + "    \"perc-max-balao\" : 50.00,\n"
                + "    \"perc-min-entrada\" : 20,\n" + "    \"perc-max-entrada\" : 50\n" + "  },\n"
                + "  \"tipoPessoa\" : \"pessoa-fisica\",\n" + "  \"itens\" : null\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        DadosSimulacao dadosSimulacao = mapper.readValue(str, DadosSimulacao.class);
        DadosSimulacaoPlanilha result = calculadoraServices.getDadosSimulacaoPlanilha(dadosSimulacao);
        assertEquals(new BigDecimal(result.getValorSeguro()), dadosSimulacao.getValorSeguroPrestamista());
    }

    @Test
    public void deveRetornaValorSeguroAutoComSeguroFranquiaEPrestamista() throws JsonProcessingException {
        String str = "{\n" + "  \"idSimulacao\" : \"FATOR_PARA_VALOR_NA_PARCELA\",\n"
                + "  \"dataSimulacao\" : [ 2019, 2, 18 ],\n" + "  \"dataPrimeiroVencimento\" : [ 2019, 6, 22 ],\n"
                + "  \"valorBem\" : 0,\n" + "  \"valorEntrada\" : 0,\n" + "  \"valorSeguroPrestamista\" : 1000,\n" + "  \"vlrSeguroFranquia\" : 4500,\n"
                + "  \"custoPercentualDoSeguroPrestamista\" : null,\n" + "  \"valorRegistroContrato\" : 0,\n"
                + "  \"valorCestaServico\" : 0,\n" + "  \"valorTC\" : 0,\n" + "  \"valorTaxaSubsidio\" : null,\n"
                + "  \"codigoRetorno\" : \"1\",\n" + "  \"periodicidade\" : 1,\n"
                + "  \"tipoFinanciamento\" : \"CDC\",\n" + "  \"tipoCalculo\" : \"TAXA\",\n"
                + "  \"valorSubsidio\" : null,\n" + "  \"temIsencaoIOF\" : false,\n" + "  \"taxaIOFAA\" : 0.030000,\n"
                + "  \"taxaIOFAD\" : 0.003800,\n" + "  \"baseCalculoIOF\" : 365,\n" + "  \"parcelasBalao\" : [ ],\n"
                + "  \"prazo\" : {\n" + "    \"prazo\" : 12,\n" + "    \"taxa-mes\" : 1.308033,\n"
                + "    \"perc-min-balao\" : 20.00,\n" + "    \"perc-max-balao\" : 50.00,\n"
                + "    \"perc-min-entrada\" : 20,\n" + "    \"perc-max-entrada\" : 50\n" + "  },\n"
                + "  \"tipoPessoa\" : \"pessoa-fisica\",\n" + "  \"itens\" : null\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        DadosSimulacao dadosSimulacao = mapper.readValue(str, DadosSimulacao.class);
        DadosSimulacaoPlanilha result = calculadoraServices.getDadosSimulacaoPlanilha(dadosSimulacao);
        assertEquals(new BigDecimal(result.getValorSeguro()), new BigDecimal(5500));
    }


    @Test
    public void deveRetornaValorSeguroAutoSemSeguroFranquiaEPrestamista() throws JsonProcessingException {
        String str = "{\n" + "  \"idSimulacao\" : \"FATOR_PARA_VALOR_NA_PARCELA\",\n"
                + "  \"dataSimulacao\" : [ 2019, 2, 18 ],\n" + "  \"dataPrimeiroVencimento\" : [ 2019, 6, 22 ],\n"
                + "  \"valorBem\" : 0,\n" + "  \"valorEntrada\" : 0,\n" + "  \"valorSeguroPrestamista\" : 0,\n" + "  \"vlrSeguroFranquia\" : 0,\n"
                + "  \"custoPercentualDoSeguroPrestamista\" : null,\n" + "  \"valorRegistroContrato\" : 0,\n"
                + "  \"valorCestaServico\" : 0,\n" + "  \"valorTC\" : 0,\n" + "  \"valorTaxaSubsidio\" : null,\n"
                + "  \"codigoRetorno\" : \"1\",\n" + "  \"periodicidade\" : 1,\n"
                + "  \"tipoFinanciamento\" : \"CDC\",\n" + "  \"tipoCalculo\" : \"TAXA\",\n"
                + "  \"valorSubsidio\" : null,\n" + "  \"temIsencaoIOF\" : false,\n" + "  \"taxaIOFAA\" : 0.030000,\n"
                + "  \"taxaIOFAD\" : 0.003800,\n" + "  \"baseCalculoIOF\" : 365,\n" + "  \"parcelasBalao\" : [ ],\n"
                + "  \"prazo\" : {\n" + "    \"prazo\" : 12,\n" + "    \"taxa-mes\" : 1.308033,\n"
                + "    \"perc-min-balao\" : 20.00,\n" + "    \"perc-max-balao\" : 50.00,\n"
                + "    \"perc-min-entrada\" : 20,\n" + "    \"perc-max-entrada\" : 50\n" + "  },\n"
                + "  \"tipoPessoa\" : \"pessoa-fisica\",\n" + "  \"itens\" : null\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        DadosSimulacao dadosSimulacao = mapper.readValue(str, DadosSimulacao.class);
        DadosSimulacaoPlanilha result = calculadoraServices.getDadosSimulacaoPlanilha(dadosSimulacao);
        assertEquals(new BigDecimal(result.getValorSeguro()), BigDecimal.ZERO);
    }


}
