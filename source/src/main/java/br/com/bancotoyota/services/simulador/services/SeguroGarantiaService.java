package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.ComboSeguroGarantiaResponse;
import br.com.bancotoyota.services.simulador.entities.SeguroGarantiaRequest;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface SeguroGarantiaService {

    /**
     * Obtem a lista de cotacoes de garantia mecanica/estendida.
     * @return lista de cotacoes.
     */
    ComboSeguroGarantiaResponse getValor(String cnpjOrigemNegocio, SeguroGarantiaRequest request);


}
