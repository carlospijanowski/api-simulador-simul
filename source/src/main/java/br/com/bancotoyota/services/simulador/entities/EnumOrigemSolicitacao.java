package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum EnumOrigemSolicitacao {
    ABRADIT("ABRADIT", "ABRADIT"),
    INSTITUCIONAL("INSTITUCIONAL", "INSTITUCIONAL"),
    LANDING_PAGE("LANDING_PAGE", "LANDING_PAGE"),
    TDB("TDB", "TDB"),
    OBO("OBO", "OBO"),
    ONEAPP("ONEAPP", "ONEAPP"),
    GERADOR_OFERTA("GERADOR_OFERTA", "GERADOR_OFERTA"),
    DIRECT("DIRECT", "DIRECT"),
    FANDI("FANDI", "FANDI");

    private String value;
    private String key;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumOrigemSolicitacao fromValue(String value){
        for (EnumOrigemSolicitacao e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("controle-balao",value);
    }

    @JsonValue
    @Override
    public String toString(){
        return this.value;
    }
}
