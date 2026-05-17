package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FatorRequest {
    @JsonProperty("plano-id")
    private Integer planoId;
    @JsonProperty("cnpj-origem-negocio")
    private String cnpjOrigemNegocio;
    @JsonProperty("cod-origem-negocio")
    private String codOrigemNegocio;
    private String rating;
}
