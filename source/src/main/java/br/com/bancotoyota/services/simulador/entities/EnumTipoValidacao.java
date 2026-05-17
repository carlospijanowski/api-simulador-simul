package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import br.com.bancotoyota.services.simulador.common.Constants;
@Getter
@AllArgsConstructor
public enum EnumTipoValidacao {
	VALIDACAO_BASICA("validacao-basica", Constants.KEY_VALIDACAO_BASICA),
	PRE_VALIDACAO_OFERTA("pre-validacao-oferta", Constants.KEY_PRE_VALIDACAO_OFERTA),
	VALIDACAO_COMPLETA("validacao-completa", Constants.KEY_VALIDACAO_COMPLETA);
	
	private String value;
    private String key;
    
    public static EnumTipoValidacao fromValue(String value){
        for (EnumTipoValidacao e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-validacao",value);
    }
}
