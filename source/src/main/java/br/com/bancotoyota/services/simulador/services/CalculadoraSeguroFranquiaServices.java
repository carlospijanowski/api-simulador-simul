package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.DadosSimulacao;
import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.DadosSimulacaoPlanilha;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;


public interface CalculadoraSeguroFranquiaServices {

    /**
     * Realiza o cálculo da parcela do seguro franquia.
     *
     * @param fator valor do calculo da parcela.
     * @param valorSeguroFranquia valor total do seguro franquia.
     * @return o resultado calculado da simulação.
     */
    SeguroFranquia calcular(BigDecimal fator, BigDecimal valorSeguroFranquia);

    /**
     * Realiza adicição de casa decimais.
     *
     * @param valor valor para ser adicionado as casa.
     * @return os resultados calculados da simulação.
     */
    BigDecimal arredondarParaDuasCasaDescimais(BigDecimal valor);

}
