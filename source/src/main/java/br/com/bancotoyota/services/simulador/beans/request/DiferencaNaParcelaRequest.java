package br.com.bancotoyota.services.simulador.beans.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
public class DiferencaNaParcelaRequest extends SimulacaoParcResidualSimplesRequest {

	@ApiModelProperty(required=true)
    @JsonProperty("valores-dos-itens")
    @NotNull
    private BigDecimal[] valoresDosItens;
}
