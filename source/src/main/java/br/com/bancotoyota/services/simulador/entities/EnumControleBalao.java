package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
@Schema(enumAsRef = true, description = "Controle balão: \n" +
        "* P - Permite Balão\n" +
        "* O - Obrigatório Balão\n" +
        "* OU - Obrigatório Última Parcela\n" +
        "* PU - Permite Balão Última Parcela\n" +
        "* NP - Não Permite Balão\n"
)
public enum EnumControleBalao {

    PERMITE_BALAO("P", "P"),
    OBRIGATORIO_BALAO("O", "O"),
    OBRIGATORIO_ULTIMA_PARCELA("OU", "OU"),
    PERMITE_BALAO_ULTIMA_PARCELA("PU", "PU"),
    NAO_PERMITE_BALAO("NP", "NP");

    private String value;
    private String key;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumControleBalao fromValue(String value){
        for (EnumControleBalao e : values()) {
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
