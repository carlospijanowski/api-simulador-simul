package br.com.bancotoyota.services.simulador.services.exceptions;

public class ServicoForaException extends RuntimeException {
    public ServicoForaException(String message, Throwable cause) {
        super(message, cause);
    }
}
