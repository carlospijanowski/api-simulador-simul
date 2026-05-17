package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class CodigoSimulacao {

    public static CodigoSimulacao of(CodigoSimulacaoPerfilGeral geral) {
        return new CodigoSimulacao(geral.getDescricaoRetorno(), geral.getDescricaoRetorno(), geral.getCodigoRetorno());
    }

    public static CodigoSimulacao of(CodigoSimulacaoPerfilEspecifico especifico) {
        return new CodigoSimulacao(especifico.getDescricaoRetorno(), especifico.getDescricaoMarkup(), especifico.getCodigoRetorno());
    }

    /**
     * Esse é o código passado para o simulador.
     */
    @JsonProperty("codigo")
    private String codigo;

    /**
     * Esse é o texto mostrado para o usuário.
     */
    @JsonProperty("descricao")
    private String descricao;

    /**
     * Esse é o ID do elemento no banco de dados e o valor passado para o Autbank.
     */
    @JsonProperty("codigo-retorno")
    private String codigoRetorno;
}
