package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum EnumItemFinanciavel {

    SERVICO_GEOLOCALIZACAO(8, "SERVIÇO GEOLOCALIZAÇÃO");

    private Integer key;
    private String value;

    public static EnumItemFinanciavel fromValue(String value) {
        for (EnumItemFinanciavel e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("controle-balao", value);
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
