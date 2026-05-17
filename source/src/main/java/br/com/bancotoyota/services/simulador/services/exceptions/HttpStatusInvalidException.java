package br.com.bancotoyota.services.simulador.services.exceptions;

import org.springframework.http.HttpStatus;

public class HttpStatusInvalidException extends Exception {

    private final HttpStatus httpStatus;

    public HttpStatusInvalidException(HttpStatus httpStatus) {
        super(httpStatus.value() + " - " + httpStatus.name());
        this.httpStatus = httpStatus;
    }

    public HttpStatusInvalidException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
