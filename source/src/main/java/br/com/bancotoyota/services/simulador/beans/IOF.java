package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class IOF {

    private BigDecimal valorIOFAA;
    private BigDecimal valorIOFAD;
    private Integer valorBaseCalculoParaIOF;

    public IOF(TaxasIOF taxasIOF) {
        valorIOFAA = new BigDecimal(taxasIOF.getValorIOFAA());
        valorIOFAD = new BigDecimal(taxasIOF.getValorIOFAD());
        valorBaseCalculoParaIOF = Integer.valueOf(taxasIOF.getValorbaseCalculo()
                .substring(0, taxasIOF.getValorbaseCalculo().indexOf('.')));
    }
}
