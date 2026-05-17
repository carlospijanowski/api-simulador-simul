package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimulacaoSimplificadaResponse {
	
	@JsonProperty("dados-simulacao-simplificada")
	private SimulacaoSimplificada dadosSimulacaoSimplificada;
	
}
