package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParcelaAvisoLegal {

    @JsonProperty("valor-taxa-cadastro")
    private BigDecimal valorTaxaCadastro;	
	
}
