package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Periodicidade {
	
	@ApiModelProperty(required=true)
	private String id;	
	
	@ApiModelProperty(required=true)
	private String descricao;
	
	@ApiModelProperty(required=true)
	private String valor;

	@ApiModelProperty(required=true)
	private UnidadePeriodicidade unidade;
}
