package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import lombok.Getter;

@Getter
public enum EnumTipoSubsidio {

    FIXO("F", "FIXO"),
    VARIAVEL("V", "FLEXIVEL/VARIAVEL");

    private String key;
    private String value;

    /*EnumTipoSubsidio(String k) {
        if(k.equalsIgnoreCase("F"))
    }*/

    EnumTipoSubsidio(String k, String v) {
        this.key = k;
        this.value = v;
    }

    public static EnumTipoSubsidio from(String value) {
        for (EnumTipoSubsidio e : values()) {
            if (e.key.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-distribuicao", value);
    }
}
