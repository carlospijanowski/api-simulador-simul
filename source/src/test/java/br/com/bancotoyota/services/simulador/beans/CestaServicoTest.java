package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.CestaServico;
import br.com.bancotoyota.services.simulador.entities.CestaItem;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;

@RunWith(SpringRunner.class)
public class CestaServicoTest {

    private CestaServico cestaServico;


    @Before
    public void setup() {
        cestaServico = new CestaServico();
        cestaServico.setValor(BigDecimal.valueOf(600.0));

        cestaServico.setCestaItems(new ArrayList<>());
        cestaServico.getCestaItems().add(new CestaItem(1, BigDecimal.valueOf(100.0), 1, "Despesa 1", false));
        cestaServico.getCestaItems().add(new CestaItem(2, BigDecimal.valueOf(200.0), 1, "Despesa 2", true));
        cestaServico.getCestaItems().add(new CestaItem(3, BigDecimal.valueOf(300.0), 1, "Despesa 3", false));
    }

    @Test
    public void test() {
        Assert.assertEquals(BigDecimal.valueOf(200.0), cestaServico.getTaxaCadastro());
        Assert.assertEquals(BigDecimal.valueOf(400.0), cestaServico.getValorSemTaxaCadastro());
    }
}
