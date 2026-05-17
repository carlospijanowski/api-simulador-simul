package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DiferencaNaParcela {

    @Schema(name = "novo-valor-parcela", description = "Novo valor de parcela do financiamento", example = "2344.56")
    @JsonProperty("novo-valor-parcela")
    private BigDecimal novoValorDaParcela;

    @Schema(name = "diferenca-valor-parcela", description = "Diferença entre valor atual e o novo", example = "123.56")
    @JsonProperty("diferenca-valor-parcela")
    private BigDecimal diferencaNoValorDaParcela;
}
