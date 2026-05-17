package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class SimulacaoFluxoResponse {

    @JsonProperty("fluxo")
    private List<Fluxo> fluxoList = new ArrayList<>();
}
