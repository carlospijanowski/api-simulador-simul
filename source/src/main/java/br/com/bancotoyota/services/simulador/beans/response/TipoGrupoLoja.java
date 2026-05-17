package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoGrupoLoja {
	
	ESPECIFICO("E","AGRUPAMENTO ESPECÍFICO")
	,ECONOMICO("N","AGRUPAMENTO ECONÔMICO")
	,REGIAO("R","REGIÃO")
	,EXCETO("X","EXCETO")
	,TODOS("T","TODOS")
	,UF("U","UF");
	
	

	private String key;
	private String value;
	
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoGrupoLoja fromValue(String value) {
    	try {
    		//Verifica se a string tem o valor do nome do construtor.
    		return TipoGrupoLoja.valueOf(value);
    	}catch (Exception ex) {
    		for (TipoGrupoLoja e : values()) {
    			//Verifica se a string tem o valor do value;
                if (e.value.equalsIgnoreCase(value)) {
                    return e;
                  //Verifica se a string tem o valor da key;
                }else if (e.key.equalsIgnoreCase(value)) {
                	return e;
                }
            }
            throw new InvalidFieldException("tipo-grupo-loja", value);
		}
        
    }

    @Override
    @JsonValue
    public String toString(){
        return this.key;
    }	

}
