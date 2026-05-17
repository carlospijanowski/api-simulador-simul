package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.AccessToken;

public interface TokenService {

	AccessToken generateAccessToken();
	AccessToken generateDirectAccessToken();
}
