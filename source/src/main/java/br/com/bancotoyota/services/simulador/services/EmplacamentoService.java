package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.Estado;
import br.com.bancotoyota.services.simulador.entities.UfEmplacamento;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.web.client.HttpStatusCodeException;

public interface EmplacamentoService {

    /**
     * Obtem os dados de emplacamento do estado pela UF informada.
     * @param uf UF do estado.
     * @param token token da requisicao.
     * @return os dados de emplacamento do estado encontrado.
     * @throws EntityNotFoundException quando o estado de emplacamento nao for encontrado.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de emplacamentos.
     */
    Estado getEstado(UfEmplacamento uf, String token);
}
