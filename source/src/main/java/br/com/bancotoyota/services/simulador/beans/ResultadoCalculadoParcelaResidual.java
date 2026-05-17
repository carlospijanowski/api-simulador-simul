package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ResultadoCalculadoParcelaResidual {

    private int prazo;
    private BigDecimal valorParcela;
    private BigDecimal valorIOF;
    private BigDecimal valorCetAnual;
    private BigDecimal valorTotalFinanciado;
    private BigDecimal valorTaxaMes;
    private BigDecimal valorTaxaAnual;
    private BigDecimal valorSubsidio;

    private CalculoSeguroMecanicaResponse dadosSeguroMecanica;

    private CustoPercentualDoSeguroPrestamista dadosSeguroPrestamista;

    private BigDecimal valorDoSeguroPrestamista;

    private BigDecimal valorDoSeguroPrestamistaNaParcela;

    private SeguroFranquia seguroFranquia;

    private BigDecimal valorParcelaResidual;

    private BigDecimal valorSeguroAutoNaParcela;

    private List<ItemFinanciavel> itensFinanciaveisNaParcela;

    private List<Fluxo> fluxo;

    private BigDecimal valorSubsidioUsado;

    private BigDecimal taxaSubsidioUsada;

    private BigDecimal valorTotalPrazo;

    private BigDecimal percentualParcelaResidual;

    /**
     * Usado para montar o fluxo no final pra evitar ter que montar o fluxo
     */
    private ResultadoCalculado resultadoCalculado;

    private BigDecimal taxaMensalCompleta;

    private BigDecimal taxaAnualCompleta;

    private BigDecimal taxaSubsidioUsadaCompleta;

    private BigDecimal valorTaxaBanco;

    /**
     * Adicionada os dadoss de cobrança da geolocalização ao resultado
     */
    private DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizaocao;
}