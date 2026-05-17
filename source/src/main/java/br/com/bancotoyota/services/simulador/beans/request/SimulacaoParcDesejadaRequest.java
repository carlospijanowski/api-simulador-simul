package br.com.bancotoyota.services.simulador.beans.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimulacaoParcDesejadaRequest extends SimulacaoRequest {

	public SimulacaoParcDesejadaRequest(SimulacaoRequest request, Integer... parcelas) {
        super(request);
        setParcelas(parcelas);
    }
	
	public SimulacaoParcDesejadaRequest(SimulacaoRequest request) {
        super(request);
    }

    @ApiModelProperty(required=true)
    @JsonProperty("valor-parcela-desejada")
    @NotNull
    @Override
    public BigDecimal getValorParcelaDesejada() {
        return super.getValorParcelaDesejada();
    }
}
