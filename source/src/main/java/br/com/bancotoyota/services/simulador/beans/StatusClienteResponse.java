package br.com.bancotoyota.services.simulador.beans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class StatusClienteResponse {

	@JsonProperty("cliente-status")
	private ClienteStatus clienteStatus;

	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	@Setter
	@EqualsAndHashCode
	public static class ClienteStatus {

		private Boolean ativo;
		
		@JsonProperty("tipo-produto")
		private TipoProdutoClienteStatus tipoProduto;
	}
	
	@Getter
	@Setter
	@NoArgsConstructor
	public static class TipoProdutoClienteStatus {

		@JsonProperty("produto")
		private List<String> produtos;
	}

}