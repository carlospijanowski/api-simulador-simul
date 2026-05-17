package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class TaxaMaximaSubsidioResponse {

    @Schema(name = "taxa-maxima-subsidio", description = "Taxa máxima de subsidio permitida no financiamento", example = "30000")
    @JsonProperty("taxa-maxima-subsidio")
    private BigDecimal taxaMaximaSubsidio;
}
