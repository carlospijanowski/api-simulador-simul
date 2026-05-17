package br.com.bancotoyota.services.simulador.beans.request;


import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.entities.ModalidadeSimplificada;
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
public class SimulacaoSimplificadaDesejadaRequest extends SimulacaoSimplificadaRequest {

    @Schema(name = "valor-parcela-desejada", description = "Valor da parcela desejado no financiamento.")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-parcela-desejada")
    @NotNull
    private BigDecimal valorParcelaDesejada;

    @Schema(name = "modalidade", description = "Modalidade do financiamento.")
    @ApiModelProperty(notes = "ciclo-toyota,cdc, credito-facil OU credito-inteligente. Quando não enviado, considera ciclo-toyota")
    @JsonProperty("modalidade")
    private ModalidadeSimplificada modalidade;

    @Schema(name = "parcelas", description = "Quantidade de parcelas disponiveis para financiamento.", example = "[ 12, 24, 36 ]")
    private Integer[] parcelas;

}
