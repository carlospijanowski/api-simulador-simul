package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Montadora {

	public Montadora(BigDecimal percentual, BigDecimal valorMaximo, BigDecimal fixoPercentual) {
		this.percentual = percentual;
		this.valorMaximo = valorMaximo;
		this.fixoPercentual = fixoPercentual;
	}

	/**
	 * Distribuição VALOR_PERCENTUAL
	 */
	private BigDecimal percentual;
	/**
	 * Distribuição VALOR_PERCENTUAL
	 */
	@JsonProperty("valor-maximo")
	private BigDecimal valorMaximo;

	/**
	 * Só para subsídio do tipo fixo.
	 */
	@JsonProperty("fixo-percentual")
	private BigDecimal fixoPercentual;

}
