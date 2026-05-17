package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import lombok.Getter;

@Getter
public enum EnumResponsabilidade {

    DEALER("D", "REVENDEDORA", 0),
    MONTADORA("M", "MONTADORA", 1);

    private String key;
    private String value;
    private int indice;

    EnumResponsabilidade(String key, String value, int indice){
        this.key = key;
        this.value = value;
        this.indice = indice;
    }

    public static EnumResponsabilidade from(String value){
        for (EnumResponsabilidade e : values()) {
            if (e.key.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-distribuicao",value);
    }
}
