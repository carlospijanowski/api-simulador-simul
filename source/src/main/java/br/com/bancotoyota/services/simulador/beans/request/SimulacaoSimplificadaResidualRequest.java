package br.com.bancotoyota.services.simulador.beans.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
public class SimulacaoSimplificadaResidualRequest extends SimulacaoSimplificadaRequest {

    @Schema(name = "valor-parcela-residual", description = "Valor da parcela residual", example = "18000")
	@ApiModelProperty(required=true)
    @JsonProperty("valor-parcela-residual")
    @NotNull
    private BigDecimal valorParcelaResidual;

    @Schema(name = "valor-subsidio", description = "Valor total do subsidio", example = "5000")
    @JsonProperty("valor-subsidio")
    private BigDecimal valorSubsidio;

    @Schema(name = "parcelas", description = "Quantidade de parcelas disponiveis para financiamento.", example = "[ 12, 24, 36 ]")
    private Integer[] parcelas;
}
