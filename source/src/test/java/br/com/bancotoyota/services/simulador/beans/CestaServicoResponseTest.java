package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.CestaServico;
import br.com.bancotoyota.services.simulador.entities.CestaServicoResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.unitils.reflectionassert.ReflectionAssert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

@RunWith(SpringRunner.class)
public class CestaServicoResponseTest {

    private CestaServicoResponse expected;

    @Before
    public void setup() {
        expected = new CestaServicoResponse(new CestaServicoResponse.Response(new ArrayList<>()));

        expected.getResponse().getItens().add(new CestaServico(13, "ISENTO FEBRABAN", new BigDecimal("0.00"), false, false, false, null));
        expected.getResponse().getItens().add(new CestaServico(16, "Isento - PF", new BigDecimal("0.00"), true, false, false, null));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        CestaServicoResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "    \"response\": {\n" +
                "        \"itens\": [\n" +
                "            {\n" +
                "                \"codigo\": 13,\n" +
                "                \"descricao\": \"ISENTO FEBRABAN\",\n" +
                "                \"valor\": 0.00,\n" +
                "                \"cesta-isenta\": false,\n" +
                "                \"cesta-travada-vendedor\": false,\n" +
                "                \"cesta-default\": false\n" +
                "            },\n" +
                "            {\n" +
                "                \"codigo\": 16,\n" +
                "                \"descricao\": \"Isento - PF\",\n" +
                "                \"valor\": 0.00,\n" +
                "                \"cesta-isenta\": true,\n" +
                "                \"cesta-travada-vendedor\": false,\n" +
                "                \"cesta-default\": false\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}"
        , CestaServicoResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
