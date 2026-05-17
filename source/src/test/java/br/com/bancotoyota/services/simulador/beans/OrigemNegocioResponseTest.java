package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocioResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.IOException;

@RunWith(SpringRunner.class)
public class OrigemNegocioResponseTest {

    private OrigemNegocioResponse expected;

    private static final String CNPJ = "03215790000110";

    @Before
    public void setup() {
        expected = new OrigemNegocioResponse(new OrigemNegocioResponse.Response());
        expected.getResponse().setOrigemNegocio(new OrigemNegocio("811", CNPJ, "S Paulo", "SP", "A",
                CNPJ, "MASAHITO VELOSOQ LEVENTER", CNPJ, "SP CAPITAL", false, true, true, true, true, true));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        OrigemNegocioResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "    \"response\": {\n" +
                "        \"origem-negocio\": {\n" +
                "            \"codigo\": \"811\",\n" +
                "            \"cnpj\": \"03215790000110\",\n" +
                "            \"cidade\": \"S Paulo\",\n" +
                "            \"estado\": \"SP\",\n" +
                "            \"status\": \"A\",\n" +
                "            \"cnpj-favorecido\": \"03215790000110\",\n" +
                "            \"nome-favorecido\": \"MASAHITO VELOSOQ LEVENTER\",\n" +
                "            \"cnpj-matriz\": \"03215790000110\",\n" +
                "            \"codigo-regiao\": \"SP CAPITAL\",\n" +
                "            \"usa-markup\": false,\n" +
                "            \"spf-disponivel\": true,\n" +
                "            \"svp-disponivel\": true,\n" +
                "            \"svp-se-spf-removido\": true,\n" +
                "            \"trava-svp\": true,\n" +
                "            \"trava-spf\": true\n" +
                "        }\n" +
                "    }\n" +
                "}"
        , OrigemNegocioResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
