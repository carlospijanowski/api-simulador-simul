package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PrazoAvisoLegal {

    @JsonProperty("parcelas")
    private Integer numeroParcelas;

    /**
     * Residual pode ser nulo no caso de CDC
     */
    private ResidualAvisoLegal residual;

    /**
     * SPF, SVP ou ""
     */
    @JsonProperty("tipo-seguro-prestamista")
    private String tipoSeguroPrestamista;

    @JsonProperty("valor-seguro-prestamista")
    private BigDecimal valorSeguroPrestamista;

    @JsonProperty("valor-total-financiado")
    private BigDecimal valorTotalFinanciado;

    @JsonProperty("valor-total-prazo")
    private BigDecimal valorTotalPrazo;

    @JsonProperty("valor-iof")
    private BigDecimal valorIOF;

    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @JsonProperty("valor-taxa-mes")
    private BigDecimal valorTaxaMes;
   
    @JsonProperty("valor-taxa-anual")
    private BigDecimal valorTaxaAnual;
   
    @JsonProperty("valor-cet-anual")
    private BigDecimal valorCetAnual;
	
}
