package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrimeiroVencimento {
	
	@JsonProperty("primeiro-vencimento-fixo")
	private Boolean primeiroVencimentoFixo;
	
	@JsonProperty("ano-primeiro-vencimento")
	private Integer anoPrimeiroVencimento;
	
	@JsonProperty("mes-primeiro-vencimento")
	private Integer mesPrimeiroVencimento;


}
