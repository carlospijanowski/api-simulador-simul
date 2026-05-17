package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.CustoPercentualDoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.DadosCalculo;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;

import java.math.BigDecimal;

public class SeguroPrestamistaPessoaJuridicaStrategy implements SeguroPrestamistaStrategy {

    @Override
    public TipoPessoa getTipoPessoa() {
        return TipoPessoa.PESSOA_JURIDICA;
    }

    @Override
    public CustoPercentualDoSeguroPrestamista getFatorDoSeguro(DadosCalculo dadosCalculo,
                                                               ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista,
                                                               BigDecimal valorFinanciado,
                                                               BigDecimal valorDaParcela,
                                                               Integer idade,
                                                               TipoPessoa tipoPessoa) {
        return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM,
                null, null, null, null);
    }
}
