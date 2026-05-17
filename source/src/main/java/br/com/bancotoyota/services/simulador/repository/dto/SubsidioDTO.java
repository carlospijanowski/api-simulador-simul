package br.com.bancotoyota.services.simulador.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class SubsidioDTO {

    private String tipoSubsidio;

    private BigDecimal taxaBanco;
    private BigDecimal percentualRevendedora;
    private BigDecimal percentualMontadora;
    private String tipoDistribuicao;
    private BigDecimal valorMaxMarca;
    private BigDecimal percentualDistrMarca;
    private Integer sequencial;
    private String responsabilidade;
    private BigDecimal valorInicial;
    private BigDecimal valorFinal;
}
