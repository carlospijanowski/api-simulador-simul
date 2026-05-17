package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import io.swagger.annotations.ApiModelProperty;
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
public class SimulacaoAvisoLegal {

	 // desejada, residual ou prazo
    @JsonProperty("tipo")
    private String tipo;
    
    @JsonProperty("prazo")
    private PrazoAvisoLegal prazo;

    @JsonProperty("item-financiavel")
    private List<ItemFinanciavel> itens;

    @JsonProperty("valor-entrada")
    private BigDecimal valorEntrada;

    @JsonProperty("valor-bem")
    private BigDecimal valorBem;

    @JsonProperty("valor-parcela")
    private BigDecimal valorParcelaDesejada;
    
    @JsonProperty("valor-residual")
    private BigDecimal valorResidual;
   
    @JsonProperty("data-base")
    @JsonFormat(pattern = "yyyyMMdd", timezone="GMT-0" )
    @ApiModelProperty(value = "AutBank")
    private LocalDate dataBase;

    private ParcelaAvisoLegal parcela;	
	
}
