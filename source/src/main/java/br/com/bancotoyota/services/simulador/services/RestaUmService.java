package br.com.bancotoyota.services.simulador.services;

import java.util.List;

import br.com.bancotoyota.services.simulador.beans.request.RestaUmRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaPrazoResponse;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaResponse;
import br.com.bancotoyota.services.simulador.beans.response.DadosAlternativa;
import br.com.bancotoyota.services.simulador.entities.ConfiguracaoPlano;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;

public interface RestaUmService {
	
	
	SimulacaoRequest getSimulacaoRequest(SimulacaoSimplificadaRequest request, ConfiguracaoPlano configuracao,
            DadosAlternativa dadosAlternativa, String perfilUsuario, String token, SimulacaoSimplificada simulacaoSimplificada, List<String> idsModalidade);

    AlternativaResponse opcaoCicloToyota(RestaUmRequest request, String perfilUsuario, String token);

    AlternativaPrazoResponse opcaoCicloToyotaAPrazo(RestaUmRequest request, String perfilUsuario, String token);
}
