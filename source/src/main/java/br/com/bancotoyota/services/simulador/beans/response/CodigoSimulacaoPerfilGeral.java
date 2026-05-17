package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class CodigoSimulacaoPerfilGeral {

    @JsonProperty("codigo-plano")
    private int codigoPlano;

    @JsonProperty("codigo-retorno")
    private String codigoRetorno;

    @JsonProperty("descricao-retorno")
    private String descricaoRetorno;
}
