package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.web.client.HttpStatusCodeException;

public interface OrigemNegocioService {

    /**
     * Obtem os dados de uma origem negocio pelo CNPJ informado.
     * @param cnpj CNPJ da origem negocio.
     * @param token token da requisicao.
     * @return os dados da origem negocio encontrada.
     * @throws EntityNotFoundException quando a origem negocio nao for encontrada.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de origens negocios.
     */
    OrigemNegocio getOrigemNegocio(String cnpj, String token);
}
