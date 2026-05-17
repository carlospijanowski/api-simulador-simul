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
public class ModeloVeiculos {
	
	@JsonProperty("ano-fabricacao")
	private String anoFabricacao;
	
	@JsonProperty("ano-modelo")
	private String anoModelo;
	
	@JsonProperty("codigo-categoria")
	private String codigoCategoria;
	
	@JsonProperty("codigo-modelo")
	private String codigoModelo;
	
	@JsonProperty("codigo-molicar")
	private String codigoMolicar;
	
	@JsonProperty("descricao")
	private String descricaoModelo;
	
	@JsonProperty("descricao-categoria")
	private String descricaoCategoria;
	
	@JsonProperty("id")
	private String idModelo;
	
	@JsonProperty("marca")
	private MarcaVeiculos marca; 
	
	@JsonProperty("tipo-registro")
	private String tipoRegistro;

}
