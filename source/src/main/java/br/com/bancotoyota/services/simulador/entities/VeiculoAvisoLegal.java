package br.com.bancotoyota.services.simulador.entities;

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
public class VeiculoAvisoLegal {

    @JsonProperty("modelo")
    private ModeloAvisoLegal modelo;

    @JsonProperty("ano-modelo")
    private Integer anoModelo;
    
    private MarcaAvisoLegal marca;

    @JsonProperty("dia-parcelas")
    private DiaParcela diaParcela;	
    
    @JsonProperty("uf-emplacamento")
    private RegistroEmplacamentoAvisoLegal registroEmplacamento;    
    
    @JsonProperty("cesta-servicos")
    private CestaAvisoLegal cesta;    
    
    private Plano plano;
	
}
