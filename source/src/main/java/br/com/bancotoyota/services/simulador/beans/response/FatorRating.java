package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatorRating implements Serializable {

    @Schema(name = "valor-taxa-banco-original", description = "Valor da taxa do banco no plano", example = "1.35")
    @JsonProperty("valor-taxa-banco-original")
    private BigDecimal valorTaxaBancoOriginal;
    @Schema(name = "fator", description = "Percentual do fator do rating de acordo com o score do crivo do cliente", example = "0.35")
    @JsonProperty("fator")
    private BigDecimal fator;
}
