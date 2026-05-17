package br.com.bancotoyota.services.simulador.utils;

import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.InternalServerError;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ErrorParser {

    private ObjectMapper mapper;

    @Autowired
    public ErrorParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<Erro> parseList(RestClientResponseException exception) {
        try {
            JavaType type = mapper.getTypeFactory().constructParametricType(List.class, Erro.class);
            return mapper.readValue(exception.getResponseBodyAsString(), type);
        } catch (IOException ex) {
            log.error("error parsing body:" + exception.getResponseBodyAsString(), ex);
            throw exception;
        }
    }

    public String parseMessage(String json) {
        try {
            JavaType type = mapper.getTypeFactory().constructParametricType(List.class, Erro.class);
            List<Erro> list = mapper.readValue(json, type);
            return list.stream().map(Erro::getMessage).reduce(null, (a,b) -> a != null ? a + ", " + b : b);
        } catch (IOException ex) {
            log.error("error parsing body:" + json, ex);
            throw new InternalServerError("error parsing error json", ex);
        }
    }
}
