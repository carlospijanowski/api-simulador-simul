package br.com.bancotoyota.services.simulador.beans.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Getter
@Setter
@EqualsAndHashCode
public class FatorSimulacao {
    private Integer planoId;
    private String cnpjOrigemNegocio;
    private String rating;
    private String origemSolicitacao;
    Set<FatorTaxaDetalhe> fatorTaxaDetalhes = new HashSet<>();
    @Bean
    public FatorSimulacao factor() {
        return new FatorSimulacao();
    }
    
    public void reset() {
    	this.planoId = null;
    	this.cnpjOrigemNegocio = "";
    	this.rating = "";
    	this.origemSolicitacao ="";
    	fatorTaxaDetalhes = new HashSet<>();
    }

}
