package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SubsidioMotorTaxa;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface MotorTaxaPlanoService {

    List<PlanoMotorTaxa> getPlanos();

    List<PlanoMotorTaxa> getPlanos(FiltrosPlano filtrosPlano);

    Taxa findTaxas(Integer planoId);

    SubsidioMotorTaxa findSubsidioFromMotorTaxa(Integer planoId);

    Subsidio findSubsidio(Integer planoId);

    List<Retorno> getRetornos(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String planoVersao);

    /**
     * Obtem o retorno de um plano de financiamento.
     * @param codigoPlano codigo do plano de financiamento.
     * @param cnpjOrigemNegocio CNPJ da origem negocio.
     * @return os retornos do plano de financiamento.
     * @throws EntityNotFoundException quando nenhum retorno for encontrado para o plano de financiamento.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario);

    /**
     * Obtem o retorno de um plano de financiamento de acordo com o codigoRetorno enviado.
     * @param codigoPlano codigo do plano de financiamento.
     * @param cnpjOrigemNegocio CNPJ da origem negocio.
     * @param codigoRetorno do retorno que deseja obter
     * @return os retornos do plano de financiamento.
     * @throws EntityNotFoundException quando nenhum retorno for encontrado para o plano de financiamento.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String codigoRetorno);


    List<Prazo> findPrazos(Integer planoId, Collection<Integer> meses);

    PlanoMotorTaxa getPlanoPorId(Integer planoId);

    Plano convertPlanoFromMotorTaxa(PlanoMotorTaxa planoResponse);

    Plano getPlanoCicloToyota(BigDecimal valorBem, BigDecimal valorEntrada, FiltrosPlano filtrosPlano);

    void applyFatorRating(SimulacaoParcResidualResponse simulacaoParcResidualResponse);
}
