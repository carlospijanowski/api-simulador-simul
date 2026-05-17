package br.com.bancotoyota.services.simulador.beans.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

@NoArgsConstructor
@Getter
@Setter
public class SimulacaoParcResidualSimplesRequest extends SimulacaoParcResidualRequest {

    /**
     * Usando array aqui só para manter compatibilidade com a classe base.
     */
    @Schema(name = "parcelas", description = "Quantidade de parcelas disponiveis para financiamento.", example = "[ 12, 24, 36 ]")
	@ApiModelProperty(required=true)
    @NotNull
    @Size(min = 1, max = 1)
    private Integer[] parcelas;
}
