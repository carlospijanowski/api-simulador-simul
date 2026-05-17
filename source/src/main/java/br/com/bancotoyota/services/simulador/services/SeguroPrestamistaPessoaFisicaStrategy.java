package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.CustoPercentualDoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.DadosCalculo;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;

import java.math.BigDecimal;

public class SeguroPrestamistaPessoaFisicaStrategy implements SeguroPrestamistaStrategy {

    @Override
    public TipoPessoa getTipoPessoa() {
        return TipoPessoa.PESSOA_FISICA;
    }

    @Override
    public CustoPercentualDoSeguroPrestamista getFatorDoSeguro(DadosCalculo dadosCalculo,
                                                               ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista,
                                                               BigDecimal valorFinanciado,
                                                               BigDecimal valorDaParcela,
                                                               Integer idade,
                                                               TipoPessoa tipoPessoa) {
        return CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(dadosCalculo, parametrosDeSeguroPrestamista,
                valorFinanciado, valorDaParcela, idade, tipoPessoa);
    }
}
