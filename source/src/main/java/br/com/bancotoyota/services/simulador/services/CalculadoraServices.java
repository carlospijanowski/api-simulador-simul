package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.DadosSimulacao;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.DadosSimulacaoPlanilha;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;

import java.util.Collection;
import java.util.List;

/**
 * Wrapper para a classe {@link br.com.bancotoyota.simuladorpropostasservices.services.ServicesSimulacao} que é onde
 * a planilha é realmente executada.
 * Esse wrapper é responsável por converter os valores de BigDecimal, como usado nesse serviço, para String,
 * como usado na classe ServicesSimulacao.
 */
public interface CalculadoraServices {

    /**
     * Realiza o cálculo de financiamento com os dados da simulação informados.
     *
     * @param dados os dados da simulação que será calculada.
     * @return o resultado calculado da simulação.
     */
    ResultadoCalculado calcular(DadosSimulacao dados);

    /**
     * Realiza os cálculos de financiamento com os dados da simulação informados.
     *
     * @param dados os dados das simulações que serão calculadas.
     * @return os resultados calculados da simulação.
     */
    List<ResultadoCalculado> calcular(Collection<DadosSimulacao> dados);

    /**
     * Obtém os dados da simulação para cálculo na planilha.
     *
     * @param dadosSimulacao dados da simulação.
     * @return os dados da simulação para cálculo na planilha.
     */
    DadosSimulacaoPlanilha getDadosSimulacaoPlanilha(DadosSimulacao dadosSimulacao);
}
