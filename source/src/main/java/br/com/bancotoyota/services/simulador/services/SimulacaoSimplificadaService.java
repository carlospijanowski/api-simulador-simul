package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaResidualResponse;
import br.com.bancotoyota.services.simulador.entities.ClienteProspect;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;

public interface SimulacaoSimplificadaService {

	String insertSimulacaoSimplificada(SimulacaoSimplificada simulacao);
	
	void updateDadosContatoBySessionId(String sessionId, String origemSolicitacao, ClienteProspect clienteProspect);
	
	void updateSimulacaoSelecionadaByCodigoSimulacao(String origemSolicitacao, ClienteProspect clienteProspect);
	
	SimulacaoSimplificada findByCodigoSimulacao(String codigoSimulacao);
	
	SimulacaoSimplificada findByCodigoSimulacaoSessionIdOrigem(String codigoSimulacao, String sessionId, String origem);
	
	SimulacaoSimplificada findByCodigoSimulacaoSessionIdOrigemPrazo(String codigoSimulacao, String sessionId, String origem, Integer prazo);
	
	SimulacaoSimplificada simplificadaDesejadaToSimulacaoSimplificada(SimulacaoSimplificada simulacaoSimplificada, SimulacaoRequest simulacaoRequest, SimulacaoSimplificadaDesejadaResponse responseDesejada, SimulacaoSimplificadaDesejadaRequest request);
	
	SimulacaoSimplificada simplificadaResidualToSimulacaoSimplificada(SimulacaoSimplificada simulacaoSimplificada, SimulacaoParcResidualRequest finalRequestDesejada, SimulacaoSimplificadaResidualResponse responseResidual, SimulacaoSimplificadaResidualRequest request);
	
}
