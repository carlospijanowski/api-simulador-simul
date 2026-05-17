package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public enum ResponsabilidadeMotorTaxa {

    DEALER("D", "REVENDEDORA"),
    MONTADORA("M", "MONTADORA");

    private String key;
    private String value;

    ResponsabilidadeMotorTaxa(String key, String value){
        this.key = key;
        this.value = value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ResponsabilidadeMotorTaxa from(String value){
        try {
            //Verifica se a string tem o valor do nome do construtor.
            return ResponsabilidadeMotorTaxa.valueOf(value);
        }catch (Exception ex) {
            for (ResponsabilidadeMotorTaxa e : values()) {
                //Verifica se a string tem o valor do value;
                if (e.value.equalsIgnoreCase(value)) {
                    return e;
                    //Verifica se a string tem o valor da key;
                }else if (e.key.equalsIgnoreCase(value)) {
                    return e;
                }
            }
            throw new InvalidFieldException("responsabilidade", value);
        }
    }

}
