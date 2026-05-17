package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsidioMotorTaxa {

    @JsonProperty("tipo-subsidio")
    private TipoSubsidio tipoSubsidio;

    @JsonProperty("tipo-distribuicao")
    private TipoDistribuicao tipoDistribuicao;

    @JsonProperty("taxa-banco")
    private BigDecimal taxaBanco;

    private Montadora montadora;
    private Revendedora revendedora;

    @JsonProperty("faixas-valor")
    private List<FaixasValorMotorTaxas> faixasValorMotorTaxas;


}
