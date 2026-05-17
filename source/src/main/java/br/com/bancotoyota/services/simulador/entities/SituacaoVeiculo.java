package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SituacaoVeiculo {

    ZERO_KM("0km"),
    USADO("usado");

    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SituacaoVeiculo fromValue(String value) {
        for (SituacaoVeiculo e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("situacao-veiculo", value);
    }

    @Override
    @JsonValue
    public String toString(){
        return this.value;
    }
}
