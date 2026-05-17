package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.CustoPercentualDoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.DadosCalculo;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;

import java.math.BigDecimal;

public class SeguroPrestamistaCalculoService {

    private final SeguroPrestamistaStrategyResolver strategyResolver;

    public SeguroPrestamistaCalculoService() {
        this(new SeguroPrestamistaStrategyResolver());
    }

    public SeguroPrestamistaCalculoService(SeguroPrestamistaStrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    public CustoPercentualDoSeguroPrestamista getFatorDoSeguro(DadosCalculo dadosCalculo,
                                                               ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista,
                                                               BigDecimal valorFinanciado,
                                                               BigDecimal valorDaParcela,
                                                               Integer idade,
                                                               TipoPessoa tipoPessoa) {
        SeguroPrestamistaStrategy strategy = strategyResolver.resolve(tipoPessoa);
        return strategy.getFatorDoSeguro(dadosCalculo, parametrosDeSeguroPrestamista, valorFinanciado,
                valorDaParcela, idade, tipoPessoa);
    }
}
