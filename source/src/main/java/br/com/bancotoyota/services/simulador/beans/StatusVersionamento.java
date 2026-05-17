package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusVersionamento {
	
	APROVADO("A")
	,DESATIVADO("D")
	,EDICAO("E")
	,PENDENTE_APROVACAO("P")
	,REJEITADO("R");

	private String value;
	
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static StatusVersionamento fromValue(String value) {
    	try {
    		return StatusVersionamento.valueOf(value);
    	}catch (Exception ex) {
    		for (StatusVersionamento e : values()) {
                if (e.value.equalsIgnoreCase(value)) {
                    return e;
                }
            }
            throw new InvalidFieldException("status-versionamento", value);
		}
        
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }	

}
