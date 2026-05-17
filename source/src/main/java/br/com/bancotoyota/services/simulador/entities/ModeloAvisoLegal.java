package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ModeloAvisoLegal {
	
	@JsonProperty("ano-modelo")
	private String anoModelo;
	
	@JsonProperty("codigo-modelo")
	private String codigoModelo;
	
	@JsonProperty("codigo-molicar")
	private String codigoMolicar;
	
	@JsonProperty("descricao")
	private String descricaoModelo;

}
