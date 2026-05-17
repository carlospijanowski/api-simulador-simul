package br.com.bancotoyota.services.simulador.services.exceptions;

public class SeguroPrestamistaNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Array de SeguroPrestamista não encontrado.";

    public SeguroPrestamistaNotFoundException(String field, String value) {
        new Erro(field, value, String.format(MESSAGE, field));
    }

}
