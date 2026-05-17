package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.PropostaAvisoLegal;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;

public interface PropostasFinanciamentoService {

	/**
	 * Gera o texto de aviso legal de uma proposta de financiamento
	 * @param Proposta com os dados da simulação de financiamento
	 * @return texto de aviso legal
	 */
	String gerarTextoAvisoLegal(PropostaAvisoLegal proposta );
	
	String gerarTextoAvisoLegalSimulacaoSimplificada( SimulacaoSimplificada simulacaoSimplificada, Integer prazoDesejado );
	
}
