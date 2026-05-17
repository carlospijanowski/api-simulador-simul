package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Parâmetros usados para obtenção da lista de possíveis planos para a resposta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoPlano {

    @Schema(name = "modalidade", description = "Modalidade do financiamento.", example = "CDC")
    private Modalidade modalidade;

    @Schema(name = "tipo-pessoa", description = "Tipo de pessoa", example = "pessoa-fisica")
    @JsonProperty("tipo-pessoa")
    private TipoPessoa tipoPessoa;

    @Schema(name = "situacao-veiculo", description = "Estado de conservação do bem.", example = "0km")
    @JsonProperty("situacao-veiculo")
    private SituacaoVeiculo situacaoVeiculo;

    @Schema(name = "venda-direta", description = "Identificador caso o financiamento seja venda direta.", example = "false")
    @JsonProperty("venda-direta")
    private boolean vendaDireta;

    @Schema(name = "subsidiado", description = "Indicada se o plano tem subsidio", example = "true")
    private boolean subsidio;

    @JsonProperty("plano-original")
    private int planoOrigianl;
}
