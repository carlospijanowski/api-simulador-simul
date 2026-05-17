package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoPessoa {

    PESSOA_FISICA("PESSOA-FISICA"),
    PESSOA_JURIDICA("PESSOA-JURIDICA");

    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TipoPessoa fromValue(String value) {
        for (TipoPessoa e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-pessoa", value);
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }
}
