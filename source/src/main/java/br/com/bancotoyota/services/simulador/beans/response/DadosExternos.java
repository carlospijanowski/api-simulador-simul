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
public class DadosExternos {
	
	@ApiModelProperty(required=true)
	@JsonProperty("codigo-base-plano")
    private Integer codigoBasePlano;
	
	@ApiModelProperty(required=true)
	@JsonProperty("codigo-modalidade")
    private Modalidade codigoModalidade;
	
	@ApiModelProperty(required=true)
	@JsonProperty("id-modalidade")
    private String idModalidade;
	
	@ApiModelProperty(required=true)
	@JsonProperty("incentivo")
    private Boolean incentivo;
	
	@ApiModelProperty(required=true)
	@JsonProperty("plus")
    private Boolean plus;
	
	@ApiModelProperty(required=true)
	@JsonProperty("carencia")
    private Boolean carencia;
	
	@ApiModelProperty(required=true)
	@JsonProperty("tarifa-cadastro")
    private Boolean tarifaCadastro;
	
	@ApiModelProperty(required=true)
	@JsonProperty("tipo-plano")
    private String tipoPlano;

	//AUTBANK (AI_AF_OU_0KM, AI_AF, AI_E_0KM, AI_AF_E_0KM)
	@ApiModelProperty(required=true)
	@JsonProperty("indicador-faixa-ano")
	private String indicadorFaixaAno;

}
