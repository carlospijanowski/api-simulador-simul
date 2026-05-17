package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.entities.ConfiguracaoPlano;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class RestaUmRequest {

	@ApiModelProperty(required=true)
    @NotNull
    private SimulacaoSimplificadaDesejadaRequest parametros;

	@ApiModelProperty(required=true)
    @NotNull
    private ConfiguracaoPlano configuracao;
}
