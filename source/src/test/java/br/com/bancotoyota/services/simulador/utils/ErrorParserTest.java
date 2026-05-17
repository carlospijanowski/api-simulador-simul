package br.com.bancotoyota.services.simulador.utils;

import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.InternalServerError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ErrorParserTest {

    private ObjectMapper mapper = new ObjectMapper();
    private ErrorParser errorParser = new ErrorParser(mapper);

    public ErrorParserTest() {
        mapper.findAndRegisterModules();
    }

    @Test
    public void parseMessage() throws Exception {
        String json = getJson("message1");
        String message2 = errorParser.parseMessage(json);
        assertEquals("message1", message2);
    }

    @Test
    public void parseMessage2() throws Exception {
        String json = getJson("message1", "message2");
        String message2 = errorParser.parseMessage(json);
        assertEquals("message1, message2", message2);
    }

    @Test(expected = InternalServerError.class)
    public void parseMessageError() throws Exception {
        String message =  "message";
        String json = getJson(message);
        errorParser.parseMessage("{"+json);
    }

    private String getJson(String message, String... messages) throws JsonProcessingException {
        Erro error = new Erro(message);
        List<Erro> list = new ArrayList<>();
        list.add(error);
        for (String msg : messages) {
            Erro error2 = new Erro(msg);
            list.add(error2);
        }
        return mapper.writeValueAsString(list);
    }

    @Test
    public void parseList() throws Exception {
        String message =  "message";
        String json = getJson(message);
        List<Erro> list = errorParser.parseList(new RestClientResponseException(json, HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, json.getBytes("UTF-8"),
                Charset.forName("UTF-8")));
        assertEquals(1, list.size());
        Erro erro = list.iterator().next();
        assertEquals(message, erro.getMessage());
    }

    @Test(expected = RestClientResponseException.class)
    public void parseListErro() throws Exception {
        String message =  "message";
        String json = "{" + getJson(message);
        errorParser.parseList(new RestClientResponseException(json, HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), null, json.getBytes("UTF-8"),
                Charset.forName("UTF-8")));
    }
}