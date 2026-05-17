package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoPlano {
	
	NORMAL("N","NORMAL"),
	CAMPANHA("C","CAMPANHA");

	private String key;
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoPlano fromValue(String value) {
    	try {
    		//Verifica se a string tem o valor do nome do construtor.
    		return TipoPlano.valueOf(value);
    	}catch (Exception ex) {
	        for (TipoPlano e : values()) {
	            if (e.value.equalsIgnoreCase(value)) {
	                return e;
	            }else if (e.key.equalsIgnoreCase(value)) {
					return e;
				}
	        }
	        throw new InvalidFieldException("tipo-plano", value);
    	}
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }	
}
