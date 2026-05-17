package br.com.bancotoyota.services.simulador.services.exceptions;

import org.springframework.http.HttpStatus;

public class ThirdPartyException extends RuntimeException {

    public ThirdPartyException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
