package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrigemNegocioResponse {

	private Response response;

	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	public static class Response {

		@JsonProperty("origem-negocio")
		private OrigemNegocio origemNegocio;
	}
}