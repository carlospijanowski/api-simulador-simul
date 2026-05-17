package br.com.bancotoyota.services.simulador.services;

import org.springframework.http.ResponseEntity;

import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;

public interface ConvertSimplificadaService {

	SimulacaoParcDesejadaResponse convertResponseResidualToDesejada(ResponseEntity<SimulacaoParcResidualResponse> responseResidual);
	
}
