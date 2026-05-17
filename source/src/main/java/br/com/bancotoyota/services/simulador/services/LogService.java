package br.com.bancotoyota.services.simulador.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class LogService {

    @Autowired
    private ObjectMapper mapper;

    private ObjectWriter writer;

    @PostConstruct
    public void config() {
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    public void logData(Object data, String message) {
        try {
            log.debug(message + ":\n" + writer.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            log.warn("erro gerando log", e);
        }
    }

    public void logHeaders(HttpHeaders headers) {
        if (headers != null) {
            StringBuilder sb = new StringBuilder("headers: \n");
            headers.entrySet().forEach(e -> sb.append("\t").append(e.getKey()).append("=").append(e.getValue()).append("\n"));
            log.debug(sb.toString());
        }
    }
}
