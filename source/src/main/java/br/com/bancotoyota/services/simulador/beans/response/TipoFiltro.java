package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoFiltro {
	
	TODOS("T","TODOS"),
	EXCETO("X","EXCETO"),
    ESPECIFICO("E","ESPECÍFICO");

	private String key;
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoFiltro fromValue(String value) {
    	
    	try {
    		return TipoFiltro.valueOf(value);
    	}catch (Exception ex) {    	
	        for (TipoFiltro e : values()) {
	            if (e.value.equalsIgnoreCase(value)) {
	                return e;
	            }else if (e.key.equalsIgnoreCase(value)) {
					return e;
				}
	        }
	        throw new InvalidFieldException("tipo-filtro", value);
    	}
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }	
}
