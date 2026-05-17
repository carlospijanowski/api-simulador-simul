package br.com.bancotoyota.services.simulador.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ConfiguracaoAvisoLegal {

    @JsonProperty("modalidade-id")
    private String modalidade;

    @JsonProperty("tipo-pessoa")
    private String tipoPessoa;

    @JsonProperty("situacao-veiculo")
    private String situacaoVeiculo;
    
    @JsonProperty("tipo-financiamento")
    public String tipoFinanciamento;
    
    
    @JsonProperty("data-calculo")
    @JsonFormat(pattern = "yyyyMMdd", timezone="GMT-0" )
    private LocalDate dataCalculo;
	
}
