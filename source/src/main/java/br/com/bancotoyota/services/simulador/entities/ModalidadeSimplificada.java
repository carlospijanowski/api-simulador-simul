package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ModalidadeSimplificada {

    CDC("CDC"),
    LEASING("LEASING"),
    CICLO_TOYOTA("CICLO-TOYOTA"),
	CREDITO_INTELIGENTE("CREDITO-INTELIGENTE"),
	CREDITO_FACIL("CREDITO-FACIL");

    @JsonValue
    private String value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ModalidadeSimplificada fromValue(String value) {
        for (ModalidadeSimplificada e : values()) {
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
