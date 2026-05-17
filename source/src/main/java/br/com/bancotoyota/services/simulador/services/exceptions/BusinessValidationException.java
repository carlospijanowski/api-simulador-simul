package br.com.bancotoyota.services.simulador.services.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Exception usada para validações que não podem ser feitas pelo Java Validation porque só podem ser feitas durante
 * a execução da lógica de negócio e por exemplo podem depender do carregamento de dados do banco de dados ou da
 * chamada de serviços e por esse motivo não devem ser feitas pelo framework de validação.
 */
@AllArgsConstructor
@Getter
public class BusinessValidationException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -2054595910043819473L;

	private final String field;

    private final String value;

    private final String message;
    
    private final String id;

    public BusinessValidationException(String message) {
        this.message = message;
        field = null;
        value = null;
        this.id = null;
    }
    
    public BusinessValidationException(String field , String value , String message) {
    	this.id = null;
    	this.field = field;
    	this.value  = value;
    	this.message = message;
    }
    
    public BusinessValidationException(String id , String message) {
        this.message = message;
        field = null;
        value = null;
        this.id = id;
    }
}
