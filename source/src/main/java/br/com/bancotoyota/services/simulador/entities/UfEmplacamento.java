package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonCreator;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;

public enum UfEmplacamento {

    AC,
    AL,
    AM,
    AP,
    BA,
    CE,
    DF,
    ES,
    GO,
    MA,
    MG,
    MS,
    MT,
    PA,
    PB,
    PE,
    PI,
    PR,
    RJ,
    RN,
    RO,
    RR,
    RS,
    SC,
    SE,
    SP,
    TO;
	
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UfEmplacamento fromValue(String value) {
        for (UfEmplacamento e : values()) {
            if (e.name().equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("uf-emplacamento", value);
    }
    
    
    
}
