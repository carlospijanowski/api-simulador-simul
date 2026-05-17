package br.com.bancotoyota.services.simulador.beans.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class SubsidioRequest {
	
	@JsonProperty("taxa-subsidio")
	private BigDecimal taxaSubsidio;
	@JsonProperty("valor-subsidio")
	private BigDecimal valorSubsidio;

}
