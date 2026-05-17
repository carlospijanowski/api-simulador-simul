package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TaxasIOF {
    @JsonProperty("base-calculo")
    private String valorbaseCalculo;
    @JsonProperty("iof-aa")
    private String valorIOFAA;
    @JsonProperty("iof-ad")
    private String valorIOFAD;
}
