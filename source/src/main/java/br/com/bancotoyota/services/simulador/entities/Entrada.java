package br.com.bancotoyota.services.simulador.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Entrada {

    @Schema(name = "cliente-prospect", description = "Dados do cliente.")
    @JsonProperty("cliente-prospect")
    private ClienteProspect cliente;

    @Schema(name = "dealer", description = "Dados da loja.")
    @JsonProperty("dealer")
    private Dealer dealer;

    @JsonProperty("veiculo-interesse")
    private Veiculo veiculo;

    @JsonProperty("detalhe-simulacao")
    private DetalheSimulacao detalheSimulacao;

    @JsonProperty("itens-financiaveis")
    private List<ItemFinanciavel> itensFinanciaveis;

    @Schema(name = "parcelas", description = "Parcelas do financiamento", example = "[\n" +
            "    48\n" +
            "  ]")
    @JsonProperty("parcelas")
    private Integer[] parcelas;

}
