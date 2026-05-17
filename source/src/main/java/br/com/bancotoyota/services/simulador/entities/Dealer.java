package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Dealer{

	@Schema(name = "cnpj-origem-negocio", description = "CNPJ da loja", example = "91919940439101")
	@JsonProperty("cnpj-origem-negocio")
	private String cnpjOrigemNegocio;

	@Schema(name = "codigo-loja", description = "Código da loja.", example = "875")
	@JsonProperty("codigo-loja")
	private String codigoLoja;

	@Schema(name = "cidade", description = "Cidade.", example = "S PAULO")
    private String cidade;

	@Schema(name = "estado", description = "Estado.", example = "SP")
    private String estado;	

	
}
