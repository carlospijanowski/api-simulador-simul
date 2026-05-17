package br.com.bancotoyota.services.simulador.beans.response;

import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class PlanosMotorTaxaResponse {

    private List<PlanoMotorTaxa> planos;

}
