package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class DadosSimulacaoParcResiduais {

    private SimulacaoParcResidualRequest request;

    private DadosCalculo dadosCalculo;

    /**
     * valor financiado para verificação de qual seguro aplicar
     */
    private BigDecimal valorFinanciado;

    @Setter
    private List<Prazo> prazos;
}
