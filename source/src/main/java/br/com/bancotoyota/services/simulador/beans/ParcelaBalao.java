package br.com.bancotoyota.services.simulador.beans;

import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Luis Santos
 * Bean com os dados da parcela balão
 */
@Getter
public class ParcelaBalao implements Serializable {
    private LocalDate data;
    private BigDecimal valor;

    public ParcelaBalao(LocalDate data, BigDecimal valor) {
        this.data = data;
        this.valor = valor;
    }
}
