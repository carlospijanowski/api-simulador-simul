package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeguroPrestamistaPlusResponse {

    @JsonProperty("seguros")
    private List<Seguro> seguros = new ArrayList<>();

    @JsonProperty("ultimo-seguro")
    private Seguro ultimoSeguro;

}
