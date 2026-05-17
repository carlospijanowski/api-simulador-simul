package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public enum PerfilUsuario {

    COORDENADOR("COORDENADOR", "01"),
    FI("F&I", "02"),
    FI_GERENTE("F&I/GERENTE", "03"),
    FI_VENDEDOR("F&I/VENDEDOR", "04"),
    FLOORPLAN("FLOORPLAN", "05"),
    GERENTE("GERENTE", "06"),
    REPRESENTANTE("REPRESENTANTE", "07"),
    VENDEDOR_GERENTE("VEND/GERENTE", "08"),
    VENDEDOR("VENDEDOR", "09"),
    RESPONSAVEL_ADM("RESPONSAVEL ADM", "10");

    private String value;
    private String key;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PerfilUsuario fromValue(String value) {
        try {
            return PerfilUsuario.valueOf(value);
        }catch (Exception ex) {
            for (PerfilUsuario e : values()) {
                if (e.value.equalsIgnoreCase(value)) {
                    return e;
                } else if (e.key.equalsIgnoreCase(value)) {
                    return e;
                }
            }
            if (value.isEmpty()) {
                return null;
            }
            throw new InvalidFieldException("perfil", value);
        }
    }

    @Override
    public String toString(){
        return this.value;
    }
}
