package br.com.bancotoyota.services.simulador.entities;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class PrazoAvisoLegalTest {

    @Test
    public void distribuicaoSubsidioFixo() {
        PrazoAvisoLegal prazoAvisoLegal =new PrazoAvisoLegal();
        prazoAvisoLegal.setNumeroParcelas(1);
        prazoAvisoLegal.setResidual(new ResidualAvisoLegal());
        prazoAvisoLegal.getResidual().setValor(new BigDecimal(100));
        prazoAvisoLegal.setTipoSeguroPrestamista("SPF");
        prazoAvisoLegal.setValorCetAnual(new BigDecimal(1));
        prazoAvisoLegal.setValorIOF(new BigDecimal(1));
        prazoAvisoLegal.setValorParcela(new BigDecimal(1000));
        prazoAvisoLegal.setValorSeguroPrestamista(new BigDecimal(1000));
        prazoAvisoLegal.setValorTaxaAnual(new BigDecimal(1000));
        prazoAvisoLegal.setValorTaxaMes(new BigDecimal(1));
        prazoAvisoLegal.setValorTotalFinanciado(new BigDecimal(1000));
        prazoAvisoLegal.setValorTotalPrazo(new BigDecimal(100));
        
       
        assertEquals(Integer.valueOf("1"), prazoAvisoLegal.getNumeroParcelas());
        assertEquals(new BigDecimal("100"), prazoAvisoLegal.getResidual().getValor());
        assertEquals("SPF", prazoAvisoLegal.getTipoSeguroPrestamista());
        assertEquals(new BigDecimal("1"), prazoAvisoLegal.getValorCetAnual());
        assertEquals(new BigDecimal("1"), prazoAvisoLegal.getValorIOF());
        assertEquals(new BigDecimal("1000"), prazoAvisoLegal.getValorParcela());
        assertEquals(new BigDecimal("1000"), prazoAvisoLegal.getValorSeguroPrestamista());
        assertEquals(new BigDecimal("1000"), prazoAvisoLegal.getValorTaxaAnual());
        assertEquals(new BigDecimal("1"), prazoAvisoLegal.getValorTaxaMes());
        assertEquals(new BigDecimal("1000"), prazoAvisoLegal.getValorTotalFinanciado());
        assertEquals(new BigDecimal("100"), prazoAvisoLegal.getValorTotalPrazo());
    }

}