package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Plano;

public interface GeolocalizacaoService {
    DadosCobrancaServicoGeolocalizacao obterDadosCobrancaServicoGeolocalizacao(SimulacaoParcResidualRequest request, Plano plano, Integer prazoSelecionado);
}