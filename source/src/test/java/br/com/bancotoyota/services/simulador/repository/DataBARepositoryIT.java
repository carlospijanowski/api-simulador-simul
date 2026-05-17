package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.entities.DataBA;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Ignore
public class DataBARepositoryIT {

    @Autowired
    private DataBARepository repository;

    @Test
    public void testeDataBA() throws Exception {
        List<DataBA> dataBA = repository.findAll();
        System.out.println(dataBA);
        Assert.assertFalse(dataBA.isEmpty());
        DataBA data = new DataBA(dataBA.iterator().next().getAtual());
        repository.save(data);
    }
}
