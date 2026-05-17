package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.Estado;
import br.com.bancotoyota.services.simulador.entities.EstadoResponse;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@RunWith(SpringRunner.class)
public class EstadoResponseTest {

    private EstadoResponse expected;

    @Before
    public void setup() {
        expected = new EstadoResponse(new EstadoResponse.Response(new ArrayList<>()));

        expected.getResponse().getEstados().add(new Estado("AC", "Todas", new BigDecimal("278.02"), "500",
                LocalDateTime.parse("2018-01-07 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER)));
        expected.getResponse().getEstados().add(new Estado("AL", "Todas", new BigDecimal("0.00"), "199",
                LocalDateTime.parse("2015-04-07 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER)));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        EstadoResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "    \"response\": {\n" +
                "        \"estados\": [\n" +
                "            {\n" +
                "                \"uf\": \"AC\",\n" +
                "                \"valor\": 278.02,\n" +
                "                \"descricao\": \"Todas\",\n" +
                "                \"codigo-registro\": \"500\",\n" +
                "                \"data-inicio\": \"2018-01-07 00:00:00.000\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"uf\": \"AL\",\n" +
                "                \"valor\": 0.00,\n" +
                "                \"descricao\": \"Todas\",\n" +
                "                \"codigo-registro\": \"199\",\n" +
                "                \"data-inicio\": \"2015-04-07 00:00:00.000\"\n" +
                "            }" +
                "        ]\n" +
                "    }\n" +
                "}"
        , EstadoResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
