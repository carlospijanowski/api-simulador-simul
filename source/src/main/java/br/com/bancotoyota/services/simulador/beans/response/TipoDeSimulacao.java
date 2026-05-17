package br.com.bancotoyota.services.simulador.beans.response;

public enum TipoDeSimulacao {
    RESIDUAL,

    /**
     * Usado quando precisamos calcular o valor na parcela para mais de um item. Em vez de fazer para cada um dos
     * itens, fazemos para o valor fixo 10 mil e aplicamos a proporção.
     */
    FATOR_PARA_VALOR_NA_PARCELA
}
