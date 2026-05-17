package br.com.bancotoyota.services.simulador.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ItemFinanciavel implements Serializable {

    @Schema(name = "codigo", description = "Codigo do item", example = "1")
	@ApiModelProperty(required=true)
	@NotNull
    private Integer codigo;

    @Schema(name = "descricao", description = "Nome do item", example = "ACESSORIOS")
    @JsonProperty("descricao")
    private String descricao;

    @Schema(name = "valor", description = "Valor total do item", example = "5000")
    @ApiModelProperty(required=true)
    @NotNull
    private BigDecimal valor;

    @Schema(name = "valor-parcela", description = "Valor do item em cada parcela", example = "240")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @Schema(name = "editavel", description = "Indica se o item pode ser editado", example = "false")
    private boolean editavel = true;

    @Schema(name = "removivel", description = "Indica se o item pode ser removivel", example = "false")
    private boolean removivel = true;
}
