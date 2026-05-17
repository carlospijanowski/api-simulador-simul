package br.com.bancotoyota.services.simulador.services.exceptions;

public class InternalServerError extends RuntimeException {

    public InternalServerError(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerError(String message) {
        super(message);
    }
}
