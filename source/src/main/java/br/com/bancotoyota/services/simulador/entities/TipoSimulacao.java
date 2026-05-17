package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoSimulacao {
    SIMPLIFICADA_DESEJADA("SIMPLIFICADA-DESEJADA"),
    SIMPLIFICADA_RESIDUAL("SIMPLIFICADA-RESIDUAL");

    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoSimulacao fromValue(String value) {
        for (TipoSimulacao e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-simulacao", value);
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }
}
