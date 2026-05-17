package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaixasValorMotorTaxas {
	
    public ResponsabilidadeMotorTaxa responsabilidade;
    public Integer sequencial;
	
	@JsonProperty("valor-final")
    public BigDecimal valorFinal;
	
	@JsonProperty("valor-inicial")
    public BigDecimal valorInicial;

}
