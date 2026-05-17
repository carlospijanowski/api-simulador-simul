package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PlanoService {

    /**
     * Obtem os planos de financiamento de acordo com os dados informados.
     * @param filtrosPlano parametro para filtrar os planos
     * @param token token da requisicao.
     * @return os planos de financiamento encontrados com os dados informados.
     * @throws EntityNotFoundException quando nenhum plano de financiamento foi encontrado.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    List<Plano> getPlanos(FiltrosPlano filtrosPlano, String token);

    /**
     * Obtem o plano de financiamento Ciclo Toyota que melhor se enquadra nos dados informados.
     * @param valorBem valor do bem.
     * @param valorEntrada valor da entrada.
     * @param filtrosPlano parametros para a filtragem dos planos
     * @param token token da requisicao.
     * @return os planos de financiamento encontrados com os dados informados.
     * @throws EntityNotFoundException quando nenhum plano de financiamento foi encontrado.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    Plano getPlanoCicloToyota(BigDecimal valorBem, BigDecimal valorEntrada, FiltrosPlano filtrosPlano, String token);

    /**
     * Obtem os retornos de um plano de financiamento.
     * @param codigoPlano codigo do plano de financiamento.
     * @param cnpjOrigemNegocio CNPJ da origem negocio.
     * @param token token da requisicao.
     * @return os retornos do plano de financiamento.
     * @throws EntityNotFoundException quando nenhum retorno for encontrado para o plano de financiamento.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    List<Retorno> getRetornos(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String token);

    /**
     * Obtem o retorno de um plano de financiamento.
     * @param codigoPlano codigo do plano de financiamento.
     * @param cnpjOrigemNegocio CNPJ da origem negocio.
     * @param token token da requisicao.
     * @return os retornos do plano de financiamento.
     * @throws EntityNotFoundException quando nenhum retorno for encontrado para o plano de financiamento.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String token);

    /**
     * Obtem o retorno de um plano de financiamento de acordo com o codigoRetorno enviado.
     * @param codigoPlano codigo do plano de financiamento.
     * @param cnpjOrigemNegocio CNPJ da origem negocio.
     * @param codigoRetorno do retorno que deseja obter
     * @param token token da requisicao.
     * @return os retornos do plano de financiamento.
     * @throws EntityNotFoundException quando nenhum retorno for encontrado para o plano de financiamento.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de planos.
     */
    Retorno getRetorno(String codigoPlano, String cnpjOrigemNegocio, String perfilUsuario, String codigoRetorno, String token);    
    
    List<Prazo> findPrazos(Integer planoId, Collection<Integer> meses);

    Subsidio findSubsidio(Integer planoId);

    Plano getPlanoById(Integer planoId);
}
