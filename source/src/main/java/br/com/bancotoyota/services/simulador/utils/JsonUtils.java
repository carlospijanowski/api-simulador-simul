package br.com.bancotoyota.services.simulador.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JsonUtils {

    private JsonUtils() {}

    private static ObjectMapper mapper = new ObjectMapper();

    public static boolean canConvert(String content, Class valueType) {
        try {
            mapper.readValue(content, valueType);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean canConvert(String content, TypeReference typeReference) {
        try {
            mapper.readValue(content, typeReference);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
