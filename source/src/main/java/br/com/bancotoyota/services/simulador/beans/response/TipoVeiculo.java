package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TipoVeiculo {
	
	private Integer id;
	
	private String descricao;
	
	private Boolean zeroKm;
	
	@ApiModelProperty(required=true)
	@JsonProperty("ano-modelo-inicio")
	private Integer anoModeloInicio;
	
	@ApiModelProperty(required=true)
	@JsonProperty("ano-modelo-final")
	private Integer anoModeloFinal;

}