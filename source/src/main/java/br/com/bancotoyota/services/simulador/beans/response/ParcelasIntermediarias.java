package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParcelasIntermediarias {

	@Schema(name = "balao-ultima", description = "Indica se o financimaneto exige residual na ultima parcela", example = "false")
	@JsonProperty("balao-ultima")
	private Boolean balaoUltima;

	@Schema(name = "obriga-balao", description = "Indica se o financimaneto exige residual", example = "false")
	@JsonProperty("obriga-balao")
	private Boolean obrigaBalao;

	@Schema(name = "quantidade-maxima-balao", description = "Numeros maximo de parcelas intermediarias", example = "6")
	@JsonProperty("quantidade-maxima-balao")
	private Integer quantidadeMaximaBalao;

	@Schema(name = "permite-balao", description = "Indica se o financiamento permite parcelas intermediarias", example = "false")
	@ApiModelProperty(required=true)
	@JsonProperty("permite-balao")
	private Boolean permiteBalao;

	@Schema(name = "percentual-minima-balao", description = "Percentual mínimo do residual", example = "20")
	@JsonProperty("percentual-minimo-balao")
	private BigDecimal percentualMinimoBalao;

	@Schema(name = "percentual-maximo-balao", description = "Percentual maximo do residual", example = "50")
	@JsonProperty("percentual-maximo-balao")
	private BigDecimal percentualMaximoBalao;

	@JsonProperty("percentuais-balao")
	private List<PercentualBalao> percentuaisBalao;

	@Schema(name = "intervalo-residual", description = "Distancia entre as parcelas intermediarias", example = "6")
	@JsonProperty("intervalo-residual")
	private Integer intervaloResidual;

	@Schema(name = "permite-balao-opcional", description = "Indica se as intermediarias são opcionais", example = "false")
	@JsonProperty("permite-balao-opcional")
	private Boolean permiteBalaoOpcional;

	@JsonProperty("restricoes-percentuais-balao")
	private List<RestricaoPercentualBalao> restricoesPercentuaisBalao;
}
