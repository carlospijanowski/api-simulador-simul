package br.com.bancotoyota.services.simulador.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Carencia {
    @Schema(name = "minimo", description = "Mínimo de dias para carência do plano", example = "5")

	@ApiModelProperty(required=true)
    @NotNull
    private Integer minimo;

    @Schema(name = "maximo", description = "Máximo de dias para carência do plano", example = "130")
	@ApiModelProperty(required=true)
    @NotNull
    private Integer maximo;
}
