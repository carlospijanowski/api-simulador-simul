package br.com.bancotoyota.services.simulador.entities;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlanosResponse {

    private Response response;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Response {

        private List<Plano> planos;
    }
}
