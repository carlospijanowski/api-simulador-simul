package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RetornoResponse {

    private Response response;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Response {

        @JsonProperty("codigo-retornos")
        private List<Retorno> retornos;
    }

}
