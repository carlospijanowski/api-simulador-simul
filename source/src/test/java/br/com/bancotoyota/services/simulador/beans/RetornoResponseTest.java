package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.Retorno;
import br.com.bancotoyota.services.simulador.entities.RetornoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.IOException;
import java.util.ArrayList;

@RunWith(SpringRunner.class)
public class RetornoResponseTest {

    private RetornoResponse expected;

    @Before
    public void setup() {
        expected = new RetornoResponse(new RetornoResponse.Response(new ArrayList<>()));

        expected.getResponse().getRetornos().add(new Retorno("0", "0", "179"));
        expected.getResponse().getRetornos().add(new Retorno("1", "1", "178"));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        RetornoResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "\"response\":{\n"+
                "        \"codigo-retornos\": [\n" +
                "            {\n" +
                "                \"codigo\": \"0\",\n" +
                "                \"descricao\": \"0\",\n" +
                "                \"codigo-retorno\": \"179\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"codigo\": \"1\",\n" +
                "                \"descricao\": \"1\",\n" +
                "                \"codigo-retorno\": \"178\"\n" +
                "            }\n" +
                "        ]\n" +
                "   }\n" +
                "}"
        , RetornoResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
