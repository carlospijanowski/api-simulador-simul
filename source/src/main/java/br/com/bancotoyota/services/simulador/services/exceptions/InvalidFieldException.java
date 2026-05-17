package br.com.bancotoyota.services.simulador.services.exceptions;

import java.util.Arrays;
import java.util.List;

public class InvalidFieldException extends RuntimeException {

    private final List<Erro> erros;
    private static final String MESSAGE = "Valor do campo %s inválido";

    public InvalidFieldException(String field, String value) {
        Erro e = new Erro(field, value, String.format(MESSAGE, field));
        this.erros = Arrays.asList(e);
    }

    public InvalidFieldException(String field, String value, String mensagem) {
        Erro e = new Erro(field, value, mensagem);
        this.erros = Arrays.asList(e);
    }

    public List<Erro> getErros() {
        return erros;
    }
}
