package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.ValorComprometidoSeguroPrestamista;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.IOException;
import java.math.BigDecimal;

@RunWith(SpringRunner.class)
public class ValorComprometidoSeguroPrestamistaTest {

    private ValorComprometidoSeguroPrestamista expected;

    @Before
    public void setup() {
        expected = new ValorComprometidoSeguroPrestamista(
                new ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro(
                        BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        ValorComprometidoSeguroPrestamista actual = new ObjectMapper().readValue(
        "{\n" +
        "  \"cliente-comprometimento-seguro\": {\n" +
        "    \"spf-valor-comprometido\": 0,\n" +
        "    \"svp-valor-comprometido\": 0\n" +
        "  }\n" +
        "}"
        , ValorComprometidoSeguroPrestamista.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
