package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropostaAvisoLegal {

	
	    private ConfiguracaoAvisoLegal configuracao;

	    private VeiculoAvisoLegal veiculo;

	    private SimulacaoAvisoLegal simulacao;

	    @JsonProperty("cnpj-origem-negocio")
	    private String cnpjOrigemNegocio;
	    
	    @JsonProperty("origem-negocio")
	    private OrigemNegocioAvisoLegal origemNegocio;

	    @JsonIgnore
	    public String getCodPlano(){
	        return veiculo.getPlano().getCodigoPlano();
	    }
	
}
