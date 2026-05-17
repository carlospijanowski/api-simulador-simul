package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import lombok.Getter;

@Getter
public enum EnumTipoDistribuicao {

    VALOR_PERCENTUAL("V", "Valor/Percentual"),
    FAIXA_VALOR("F", "Faixa de Valor");

    private String key;
    private String value;

    EnumTipoDistribuicao(String k, String v){
        this.key = k;
        this.value = v;
    }


    public static EnumTipoDistribuicao from(String value){
        for (EnumTipoDistribuicao e : values()) {
            if (e.key.equalsIgnoreCase(value)) {
                return e;
            }
        }
        throw new InvalidFieldException("tipo-distribuicao",value);
    }

}
