package br.com.bancotoyota.services.simulador.beans.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DiferencaNaParcelaResponse {

    private DiferencaNaParcela[] diferencas;
}
