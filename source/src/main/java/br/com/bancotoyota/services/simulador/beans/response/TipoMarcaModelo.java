package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoMarcaModelo {

	ESPECIFICO("E","ESPECÍFICO"), //CODIGO MODELO
    LIKE("L","LIKE"), //SOMENTE NO COMEÇO DA PALAVRA
    EXCETO("X","EXCETO"), //CODIGO MODELO
    TODOS("T","TODOS");

    private String key;
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoMarcaModelo fromValue(String value) {
    	try {
    		return TipoMarcaModelo.valueOf(value);
    	}catch (Exception ex) { 
	        for (TipoMarcaModelo e : values()) {
	            if (e.value.equalsIgnoreCase(value)) {
	                return e;
	            }else if (e.key.equalsIgnoreCase(value)) {
                    return e;
                }
	        }
	        throw new InvalidFieldException("tipo-marca-modelo", value);
    	}
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }	
	
}
