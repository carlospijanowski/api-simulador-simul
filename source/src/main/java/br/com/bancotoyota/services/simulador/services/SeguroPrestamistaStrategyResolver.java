package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.TipoPessoa;

import java.util.EnumMap;
import java.util.Map;

public class SeguroPrestamistaStrategyResolver {

    private final SeguroPrestamistaStrategy defaultStrategy;
    private final Map<TipoPessoa, SeguroPrestamistaStrategy> strategies;

    public SeguroPrestamistaStrategyResolver() {
        this(new SeguroPrestamistaPessoaFisicaStrategy(), new SeguroPrestamistaPessoaJuridicaStrategy());
    }

    public SeguroPrestamistaStrategyResolver(SeguroPrestamistaStrategy pessoaFisicaStrategy,
                                             SeguroPrestamistaStrategy pessoaJuridicaStrategy) {
        this.defaultStrategy = pessoaFisicaStrategy;
        this.strategies = new EnumMap<>(TipoPessoa.class);
        this.strategies.put(pessoaFisicaStrategy.getTipoPessoa(), pessoaFisicaStrategy);
        this.strategies.put(pessoaJuridicaStrategy.getTipoPessoa(), pessoaJuridicaStrategy);
    }

    public SeguroPrestamistaStrategy resolve(TipoPessoa tipoPessoa) {
        return strategies.getOrDefault(tipoPessoa, defaultStrategy);
    }
}
