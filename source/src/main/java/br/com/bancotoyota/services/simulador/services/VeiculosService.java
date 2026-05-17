package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.ModeloVeiculos;

public interface VeiculosService {
	
	/*
	 * Retorna o modelo de veiculo através da marcaId modeloId e anoModelo
	 */
	ModeloVeiculos getModelo(String marcaId, String modeloId, Integer anoModelo, String token);
	
	
	/*
	 * Busca o modelo de veiculo através da marcaId modeloId e anoModelo do ano 9999 (veículo 0km), caso não encontre
	 * , faz uma nova busca com o ano atual  
	 */	
	ModeloVeiculos getModeloZeroKm(String marcaId, String modeloId, Integer anoModeloZeroKm, String token);
	
}
