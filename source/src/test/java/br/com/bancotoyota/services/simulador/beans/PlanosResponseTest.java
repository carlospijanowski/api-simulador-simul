package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;
import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.entities.PlanosResponse;
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
import java.util.Arrays;

@RunWith(SpringRunner.class)
public class PlanosResponseTest {

    private PlanosResponse expected;

    @Before
    public void setup() {
        expected = new PlanosResponse(new PlanosResponse.Response(new ArrayList<>()));

        expected.getResponse().getPlanos().add(new Plano("ciclo-toyota", "2838",
                "2738", "CICLO LXT>30 121", 2018, 9999,
                LocalDateTime.parse("2019-03-14 00:00:00.000", LocalDateTimeSerializer.DATE_TIME_FORMATTER),
                new Plano.UsoPlanoObject(Arrays.asList("T"), Arrays.asList("002")), new BigDecimal("30.000000"),
                new BigDecimal("100.000000"), new BigDecimal("0.00"), new BigDecimal("50.00"),
                10, 90, "CDC-TCM", false,
                null, false, "AI_E_0KM", 30, "12", "N", "MENSAL",
                1, true, false,EnumControleBalao.PERMITE_BALAO,null,false,null));
    }

    @Test
    public void deveConverterJsonParaObjeto() throws IOException {
        PlanosResponse actual = new ObjectMapper().readValue(
        "{\n" +
                "    \"response\": {\n" +
                "        \"planos\": [\n" +
                "            {\n" +
                "                \"codigo-modalidade\": \"ciclo-toyota\",\n" +
                "                \"codigo-plano\": \"2838\",\n" +
                "                \"codigo-base-plano\": \"2738\",\n" +
                "                \"descricao\": \"CICLO LXT>30 121\",\n" +
                "                \"ano-modelo-inicio\": 2018,\n" +
                "                \"ano-modelo-final\": 9999,\n" +
                "                \"data-inicial-vigencia\": \"2019-03-14 00:00:00.000\",\n" +
                "                \"uso-plano\": {\n" +
                "                    \"uso-plano\": [\"T\"],\n" +
                "                    \"codigo-marca\": [\"002\"]\n" +
                "                },\n" +
                "                \"parcela-minima-entrada\": 30.000000,\n" +
                "                \"parcela-maxima-entrada\": 100.000000,\n" +
                "                \"parcela-minima-balao\": 0.00,\n" +
                "                \"parcela-maxima-balao\": 50.00,\n" +
                "                \"quantidade-minima-carencia\": 10,\n" +
                "                \"quantidade-maxima-carencia\": 90,\n" +
                "                \"tipo-plano\": \"CDC-TCM\",\n" +
                "                \"plano-subsidiado\": false,\n" +
                "                \"primeiro-vencimento-fixo\": false,\n" +
                "                \"indicador-faixa-ano\": \"AI_E_0KM\",\n" +
                "                \"prazo-validade\": 30,\n" +
                "                \"isenta-iof\" : \"N\",\n" +
                "                \"periodicidade-numero\" : 1,\n" +
                "                \"periodicidade\" : \"MENSAL\",\n" +
                "                \"balao-ultima\" : false,\n" +
                "                \"controle-balao\" : \"P\",\n" +
                "                \"permite-balao\": true,\n" +
                "                \"id-modalidade\": \"12\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}\n"
        , PlanosResponse.class);

        ReflectionAssert.assertReflectionEquals(expected, actual);
    }
}
