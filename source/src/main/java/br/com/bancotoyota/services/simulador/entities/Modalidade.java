package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Modalidade {

    CDC("CDC"),
    LEASING("LEASING"),
    CICLO_TOYOTA("CICLO-TOYOTA");

    @JsonValue
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Modalidade fromValue(String value) {
        for (Modalidade e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("modalidade-id", value);
    }

    @Override
    public String toString(){
        return this.value;
    }
}
