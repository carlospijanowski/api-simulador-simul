package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MotorTaxaPlanosResponse {

    private Response response;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Response {

        private List<PlanoMotorTaxa> planos;
    }
}
