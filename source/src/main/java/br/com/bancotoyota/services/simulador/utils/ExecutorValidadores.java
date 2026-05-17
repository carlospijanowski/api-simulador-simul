package br.com.bancotoyota.services.simulador.utils;

import br.com.bancotoyota.services.simulador.validadores.ValidadorBalaoModalidade;

import java.util.List;

public class ExecutorValidadores {

    public static void executarValidadoresBalao(List<ValidadorBalaoModalidade> validadorBalaoModalidade) {
        validadorBalaoModalidade.stream().forEach(validador -> validador.validar());
    }
}
