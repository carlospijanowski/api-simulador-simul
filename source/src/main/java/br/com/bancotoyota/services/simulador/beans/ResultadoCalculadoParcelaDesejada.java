package br.com.bancotoyota.services.simulador.beans;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResultadoCalculadoParcelaDesejada extends ResultadoCalculadoParcelaResidual {

    public ResultadoCalculadoParcelaDesejada(
            ResultadoCalculadoParcelaResidual r,
            BigDecimal minPercResidual,
            BigDecimal maxPercResidual) {
        super(r.getPrazo(), r.getValorParcela(), r.getValorIOF(), r.getValorCetAnual(), r.getValorTotalFinanciado(),
                r.getValorTaxaMes(), r.getValorTaxaAnual(), r.getValorSubsidio(), r.getDadosSeguroMecanica() ,r.getDadosSeguroPrestamista(),
                r.getValorDoSeguroPrestamista(), r.getValorDoSeguroPrestamistaNaParcela(),
                r.getSeguroFranquia(),
                r.getValorParcelaResidual(), r.getValorSeguroAutoNaParcela(), r.getItensFinanciaveisNaParcela(),
                r.getFluxo(), r.getValorSubsidioUsado(), r.getTaxaSubsidioUsada(), r.getValorTotalPrazo(), r.getPercentualParcelaResidual(), r.getResultadoCalculado(),
                r.getTaxaMensalCompleta(), r.getTaxaAnualCompleta(), r.getTaxaSubsidioUsadaCompleta(), r.getValorTaxaBanco(), r.getDadosCobrancaServicoGeolocalizaocao());
        this.minPercResidual = minPercResidual;
        this.maxPercResidual = maxPercResidual;
    }

    private BigDecimal minPercResidual;
    private BigDecimal maxPercResidual;
}
