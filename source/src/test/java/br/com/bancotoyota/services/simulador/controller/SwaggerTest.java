package br.com.bancotoyota.services.simulador.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.diff.Delta;
import org.assertj.core.util.diff.DiffUtils;
import org.assertj.core.util.diff.Patch;
import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"swagger"})
public class SwaggerTest {

    private static final String SWAGGER_JSON = "simulador-financiamentos.json";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void compararSwagger() throws Exception {

        ResponseEntity<String> response = restTemplate.exchange("/v3/api-docs", HttpMethod.GET,null,
                String.class);

        ObjectMapper mapper = new ObjectMapper();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String newSwagger = response.getBody();
        newSwagger = reformatar(newSwagger, true);

        FileInputStream fin = new FileInputStream("../design/" + SWAGGER_JSON);
        String oldSwagger = IOUtils.toString(fin, "UTF-8");
        fin.close();
        oldSwagger = reformatar(oldSwagger, false);

        boolean swaggersSemDiferenca = mapper.readTree(newSwagger).equals(mapper.readTree(oldSwagger));
        if (!swaggersSemDiferenca) {
            List<String> list = converterParaLista(newSwagger);
            List<String> listOriginal = converterParaLista(oldSwagger);
            Patch<String> patch = DiffUtils.diff(listOriginal, list);
            List<Delta<String>> diffs = patch.getDeltas();
            System.out.println("número de diferenças: " + diffs.size());
            diffs.forEach(d -> System.out.println(d.getOriginal().getLines() + " -> " + d.getRevised().getLines()));
            System.out.println("---------------------");
            if (!diffs.isEmpty()) {
                if (true) {
                    writeFile(newSwagger, ".json");
                    writeFile(oldSwagger, "");
                } else {
                    System.out.println("novo swagger:\n" + newSwagger);
                }
            }

//        assertEquals(0, diffs.size());
        }

        assertTrue(swaggersSemDiferenca);
    }

    private void writeFile(String newSwagger, String sufix) throws IOException {
        FileWriter fw = new FileWriter("../design/" + SWAGGER_JSON + sufix);
        newSwagger = newSwagger.replaceAll("\r\n","\n");
        fw.write(newSwagger);
        fw.close();
    }

    private String reformatar(String json, boolean fix) throws JsonProcessingException {
        Document document = Document.parse(json);
        if (fix) {
            Document node = (Document) document.get("paths");
            node = (Document) node.get("/data-calculo");
            node = (Document) node.get("put");
            node = (Document) node.get("responses");
            node.remove("200");
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    }

    private List<String> converterParaLista(String novaPropostaStr) throws IOException {
        List<String> list = new ArrayList<>();
        try (StringReader stringReader = new StringReader(novaPropostaStr);
             BufferedReader reader = new BufferedReader(stringReader)) {
            String str;
            while ((str = reader.readLine()) != null) {
                list.add(str);
            }
        }
        return list;
    }
}
