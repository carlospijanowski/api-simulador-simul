package br.com.bancotoyota.services.simulador.config;

import br.com.bancotoyota.services.simulador.common.Constants;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Component
@Profile("cds") // usado só para pegar a lista de classes para o CDS
@Slf4j
public class CDSOnStartServer implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RestTemplate template;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    log.info("aplicação iniciada, parando a aplicação para gerar a lista de classes");
                    MultiValueMap<String, String> headers = new HttpHeaders();
                    headers.put("content-type", Arrays.asList("application/json"));
                    HttpEntity<String> entity = new HttpEntity<>(Constants.JSON_SIMULACAO, headers);
                    try {
                        ResponseEntity<Object> obj = template.exchange("http://localhost:8080/financiamentos/parcelas/residual/simulacao",
                                HttpMethod.POST, entity, Object.class);
                        log.info("http status: " + obj.getStatusCode());
                    } catch (Exception ex) {
                        log.error("erro ao fazer a chamada: " + ex.getMessage());
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
                SpringApplication.exit(event.getApplicationContext());
            }
        };
        t.setDaemon(true);
        t.start();
    }
}