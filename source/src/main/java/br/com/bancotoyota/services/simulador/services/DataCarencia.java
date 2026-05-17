package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;

import java.time.LocalDate;

public interface DataCarencia {
    boolean isDentroCarencia(SimulacaoRequest request, LocalDate dataCalculo);

    /**
     * Método somente usado para facilitar fazer o mock dos testes com datas fixas.
     */
    LocalDate getDataCalculo();

    void setDataCalculo(LocalDate dataBa);
}
