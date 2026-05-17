package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.Subsidio;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class ResultadoParcelaResidual {

    private List<ResultadoCalculadoParcelaResidual> list;

    private Subsidio dadosDoSubsidio;
}
