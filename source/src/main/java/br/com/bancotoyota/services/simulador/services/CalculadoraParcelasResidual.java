package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.*;

import java.time.*;
import java.util.*;

public interface CalculadoraParcelasResidual {

    ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                            boolean somenteFluxo, LocalDate dataCalculo, boolean validarResidual);


    ResultadoParcelaResidual executarSimulacao(boolean somenteFluxo, List<Prazo> prazos,
                                               TipoDeSeguroPrestamista tipoFixo, SimulacaoParcResidual simulacao);

    SimulacaoParcResidual criarSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                         List<Prazo> prazos, boolean corrigirMaximo, LocalDate dataCalculo, boolean validarResidual);
}
