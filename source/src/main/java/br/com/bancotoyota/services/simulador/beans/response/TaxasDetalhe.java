package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxasDetalhe {
	
	@ApiModelProperty(required=true,notes="Percentual Mínimo de Entrada (maior ou igual)")
	@JsonProperty("percentual-minimo-entrada")
	private BigDecimal percentualMinimoEntrada;
	
	@ApiModelProperty(required=true,notes="Percentual Máximo de Entrada (menor)")
	@JsonProperty("percentual-maximo-entrada")
	private BigDecimal percentualMaximoEntrada;
	
	@ApiModelProperty(required=false,notes="Percentual Mínimo de Balão (maior ou igual)")
	@JsonProperty("percentual-minimo-balao")
	private BigDecimal percentualMinimoBalao;
	
	@ApiModelProperty(required=false,notes="Percentual Máximo de Balão (menor)")
	@JsonProperty("percentual-maximo-balao")
	private BigDecimal percentualMaximoBalao;
	
	@JsonProperty("prazo-minimo")
	private Integer prazoMinimo;
	
	@JsonProperty("prazo-maximo")
	private Integer prazoMaximo;
	
	@JsonProperty("taxa-mes")
	private BigDecimal taxaMes;

}
