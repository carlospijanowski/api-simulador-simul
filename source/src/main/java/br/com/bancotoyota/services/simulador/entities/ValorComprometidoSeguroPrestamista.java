package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ValorComprometidoSeguroPrestamista {

	@JsonProperty("cliente-comprometimento-seguro")
	private ClienteComprometimentoSeguro clienteComprometimentoSeguro;

	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static class ClienteComprometimentoSeguro {

		@JsonProperty("spf-valor-comprometido")
		private BigDecimal spfValorComprometido;

		@JsonProperty("svp-valor-comprometido")
		private BigDecimal svpValorComprometido;
	}
}