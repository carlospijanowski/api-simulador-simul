package br.com.bancotoyota.services.simulador.beans.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FatorTaxaDetalhe {
    private Integer parcela;
    private BigDecimal valorTaxaBancoOriginal;
    private BigDecimal valorTaxaEfetiva;
    private BigDecimal fator;
}
