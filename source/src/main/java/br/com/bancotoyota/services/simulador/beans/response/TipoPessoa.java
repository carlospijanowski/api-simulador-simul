package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoPessoa {
	
	PESSOA_FISICA("F","PESSOA-FISICA"),
	PESSOA_JURIDICA("J","PESSOA-JURIDICA"),
	AMBOS("A","AMBOS");

	private String key;
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoPessoa fromValue(String value) {
    	try {
    		//Verifica se a string tem o valor do nome do construtor.
    		return TipoPessoa.valueOf(value);
    	}catch (Exception ex) {
	        for (TipoPessoa e : values()) {
	            if (e.value.equalsIgnoreCase(value)) {
	                return e;
	            }else if (e.key.equalsIgnoreCase(value)) {
	            	return e;
	            }
	        }
	        throw new InvalidFieldException("tipo-pessoa", value);
    	}
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }	
}
