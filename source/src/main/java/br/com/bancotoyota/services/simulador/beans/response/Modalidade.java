package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static br.com.bancotoyota.services.simulador.beans.response.Constants.*;

@AllArgsConstructor
@Getter
public enum Modalidade {

    CDC("cdc", KEY_CDC),
    LEASING("leasing", KEY_LEASING),
    CICLO_TOYOTA("ciclo-toyota", KEY_CICLO_TOYOTA);

    private String value;
    private String key;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Modalidade fromValue(String value){
        try {
            return Modalidade.valueOf(value);
        }catch (Exception ex) {
            for (Modalidade e : values()) {
                if (e.value.equalsIgnoreCase(value)) {
                    return e;
                }else if (e.key.equalsIgnoreCase(value)) {
                    return e;
                }
            }
            throw new InvalidFieldException("modalidade_id",value);
        }
    }

    @JsonValue
    @Override
    public String toString(){
        return this.value;
    }
}
