package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.Subsidio;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class LimitesSubsidioValor {

    private Map<Integer, BigDecimal> limitePorPrazo;

    private Subsidio dadosDoSubsidio;
}
