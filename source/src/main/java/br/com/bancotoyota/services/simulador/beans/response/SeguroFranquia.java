package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class SeguroFranquia {
    @Schema(name = "valor-total", description = "Valor total do seguro franquia.", example = "12000")
    @JsonProperty("valor-total")
    private BigDecimal valorTotal;

    @Schema(name = "valor-parcela", description = "Valor de cada parcela do seguro franquia.", example = "200")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @Schema(name = "cnpj-seguradora", description = "CPNJ da seguradora.", example = "33016221000107")
    @JsonProperty("cnpj-seguradora")
    private String cnpjSeguradora;
}
