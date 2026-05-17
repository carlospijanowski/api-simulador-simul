package br.com.bancotoyota.services.simulador.utils;

import br.com.bancotoyota.services.simulador.entities.MessageHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JsonUtilsTest {

    @Test
    public void deveRetornarTrue() {
        Assert.assertTrue(JsonUtils.canConvert("{\"message\":\"\"}", MessageHolder.class));
        Assert.assertTrue(JsonUtils.canConvert("[{\"message\":\"\"}]", new TypeReference<List<MessageHolder>>(){}));
    }

    @Test
    public void deveRetornarFalse() {
        Assert.assertFalse(JsonUtils.canConvert("{\"mensagem\":\"\"}", MessageHolder.class));
        Assert.assertFalse(JsonUtils.canConvert("[{\"mensagem\":\"\"}]", new TypeReference<List<MessageHolder>>(){}));
    }
}
