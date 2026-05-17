package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.CestaItem;
import br.com.bancotoyota.services.simulador.entities.DespesaResponse;
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
public class CestaItemResponseTest {

    private DespesaResponse expected;

    @Before
    public void setup() {
        expected = new DespesaResponse(new DespesaResponse.Response(new ArrayList<>()));

    expected.getResponse().getItens().add(new CestaItem(1, new BigDecimal("550.00"), 1, "CONFECÇÃO DE CADASTRO - PF", true));
        expected.getResponse().getItens().add(new CestaItem(3, new BigDecimal("320.00"), 1, "TARIFA DE ADITAMENTOS", false));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        DespesaResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "    \"response\": {\n" +
                "        \"itens\": [\n" +
                "            {\n" +
                "                \"codigo-despesa\": 1,\n" +
                "                \"valor-unitario\": 550.00,\n" +
                "                \"quantidade\": 1,\n" +
                "                \"descricao\": \"CONFECÇÃO DE CADASTRO - PF\",\n" +
                "                \"isenta-cliente-ativo\": true\n" +
                "            },\n" +
                "            {\n" +
                "                \"codigo-despesa\": 3,\n" +
                "                \"valor-unitario\": 320.00,\n" +
                "                \"quantidade\": 1,\n" +
                "                \"descricao\": \"TARIFA DE ADITAMENTOS\",\n" +
                "                \"isenta-cliente-ativo\": false\n" +
                "            }" +
                "        ]\n" +
                "    }\n" +
                "}"
        , DespesaResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
