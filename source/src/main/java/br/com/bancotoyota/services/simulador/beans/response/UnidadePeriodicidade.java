package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UnidadePeriodicidade {

	DIA("D", "DIA"),
	MES("M", "MÊS"),
	ANO("A", "ANO");

    private String key;
	private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UnidadePeriodicidade fromValue(String value) {
    	try {
    		//Verifica se a string tem o valor do nome do construtor.
    		return UnidadePeriodicidade.valueOf(value);
    	}catch (Exception ex) {    	
	        for (UnidadePeriodicidade e : values()) {
	            if (e.value.equalsIgnoreCase(value)) {
	                return e;
	            }else if (e.key.equalsIgnoreCase(value)) {
					return e;
				}
	        }
	        throw new InvalidFieldException("unidade-periodicidade", value);	
    	}
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }		
	
}
