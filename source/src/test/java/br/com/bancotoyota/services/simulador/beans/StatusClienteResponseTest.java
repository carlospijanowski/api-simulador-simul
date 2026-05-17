package br.com.bancotoyota.services.simulador.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class StatusClienteResponseTest {

    private StatusClienteResponse expected;

    @Before
    public void setup() {
        expected = new StatusClienteResponse(new StatusClienteResponse.ClienteStatus(true, null));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws Exception {
        StatusClienteResponse actual = new ObjectMapper().readValue(
        "{\n" +
        "  \"cliente-status\": {\n" +
        "    \"ativo\": true\n" +
        "  }\n" +
        "}"
        , StatusClienteResponse.class);

        Assert.assertEquals(expected, actual);
    }
}
