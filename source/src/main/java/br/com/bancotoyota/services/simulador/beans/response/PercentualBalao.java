package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PercentualBalao {

    @Schema(name = "prazo-inicial", description = "Número da parcela inicial da intermediaria", example = "12")
    @JsonProperty("prazo-inicial")
    private int prazoInicial;

    @Schema(name = "prazo-final", description = "Número da parcela final da intermediaria", example = "23")
    @JsonProperty("prazo-final")
    private int prazoFinal;

    @Schema(name = "minimo", description = "Percentual minimo na intermediaria", example = "12")
    @EqualsAndHashCode.Exclude
    @JsonProperty("minimo")
    private BigDecimal minimo;

    @Schema(name = "maximo", description = "Percentual maximo na intermediaria", example = "12")
    @EqualsAndHashCode.Exclude
    @JsonProperty("maximo")
    private BigDecimal maximo;
}
