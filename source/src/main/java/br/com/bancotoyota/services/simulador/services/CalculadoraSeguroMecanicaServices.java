package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;

public interface CalculadoraSeguroMecanicaServices {

    CalculoSeguroMecanicaResponse calcular(ParametrosSeguroMecanica parametrosSeguroMecanica, Integer prazo);

}
