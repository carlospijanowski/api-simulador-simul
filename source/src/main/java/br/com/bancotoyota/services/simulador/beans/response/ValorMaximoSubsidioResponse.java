package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class ValorMaximoSubsidioResponse {

    @JsonProperty("valor-maximo-subsidio")
    private BigDecimal valorMaximoSubsidio;
}
