package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.*;

import java.time.*;
import java.util.*;

public interface CalculadoraOfertas {

    void adicionarIntermediariasNoCalculoDasOfertas(SimulacaoParcResidualRequest request, ResultadoParcelaResidual resultadoSimulacao
            , boolean validarSubsidio, boolean somenteFluxo, boolean corrigirMaximo, LocalDate dataCalculo, List<Prazo> prazos
            , TipoDeSeguroPrestamista tipoFixo, boolean validarResidual, SimulacaoParcResidual simulacao);

}
