package br.com.bancotoyota.services.simulador.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PercentualParcelaResidual {

    private BigDecimal percentual;

    private BigDecimal maxPercentualParcelaResidualAnterior;
}
