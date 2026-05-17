package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.DadosSimulacao;
import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.DadosSimulacaoPlanilha;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.properties")
public class CalculadoraSeguroFranquiaServicesImplTest {



    private CalculadoraSeguroFranquiaServicesImpl calculadoraServices;

    @Before
    public void setup() throws JsonProcessingException {
        calculadoraServices = new CalculadoraSeguroFranquiaServicesImpl();
    }

    @Test
    public void testCalcular_ValoresValidos() throws JsonProcessingException {

        BigDecimal valorSeguroFranquia = new BigDecimal(4500);
        BigDecimal fator = new BigDecimal(0.041900743);
        SeguroFranquia result = calculadoraServices.calcular(fator,valorSeguroFranquia);
        assertEquals(result.getValorTotal(), new BigDecimal("4500.00"));
        assertEquals(result.getValorParcela(), new BigDecimal("188.55"));
    }

    @Test
    public void testCalcular_ValorSeguroFranquiaNulo() throws JsonProcessingException {

        BigDecimal valorSeguroFranquia = BigDecimal.ZERO;
        BigDecimal fator = new BigDecimal(0.041900743);
        SeguroFranquia result = calculadoraServices.calcular(fator,valorSeguroFranquia);
        assertEquals(result.getValorTotal(), new BigDecimal("0.00"));
        assertEquals(result.getValorParcela(), new BigDecimal("0.00"));
    }

    @Test
    public void testAdicionarCasaDescimais() {
        BigDecimal valor = BigDecimal.valueOf(100.567);
        CalculadoraSeguroFranquiaServicesImpl service = new CalculadoraSeguroFranquiaServicesImpl();
        BigDecimal resultado = calculadoraServices.arredondarParaDuasCasaDescimais(valor);
         assertEquals(BigDecimal.valueOf(100.57), resultado);
    }

        @Test

        public void testAdicionarCasaDescimais_ArredondamentoParaBaixo() {
            BigDecimal valor = BigDecimal.valueOf(100.564);
            CalculadoraSeguroFranquiaServicesImpl service = new CalculadoraSeguroFranquiaServicesImpl();
            BigDecimal resultado = calculadoraServices.arredondarParaDuasCasaDescimais(valor);
            assertEquals(BigDecimal.valueOf(100.56), resultado);
        }
}
