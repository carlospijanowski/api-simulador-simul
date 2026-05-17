package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.entities.CestaServico;
import br.com.bancotoyota.services.simulador.entities.CestaItem;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.List;

public interface CestaServicoService {

    /**
     * Obtem as cestas de servicos de uma origem negocio.
     * @param cnpj CNPJ da oriem negocio.
     * @param modalidade modalidade de financiamento.
     * @param tipoPessoa tipo da pessoa.
     * @param token token da requisicao.
     * @return as cestas de servicos da origem negocio.
     * @throws EntityNotFoundException quando nenhuma cesta de servico for encontrada.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de cestas de servicos.
     */
    List<CestaServico> getCestasServicos(String cnpj, Modalidade modalidade, TipoPessoa tipoPessoa,
                                         String perfilUsuario, String token);

    /**
     * Obtem a cesta de servico mais cara de uma origem negocio.
     * @param cnpj CNPJ da oriem negocio.
     * @param modalidade modalidade de financiamento.
     * @param tipoPessoa tipo da pessoa.
     * @param token token da requisicao.
     * @return a cesta de servico mais cara da origem negocio.
     * @throws EntityNotFoundException quando nenhuma cesta de servico for encontrada.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de cestas de servicos.
     */
    CestaServico getCestaServicoMaisCara(String cnpj, Modalidade modalidade, TipoPessoa tipoPessoa,
                                         String perfilUsuario, String token);

    /**
     * Obtem as despesas de uma cesta de servico.
     * @param codigoCesta codigo da cesta.
     * @param token token da requisicao.
     * @return as despesas da cesta de serviço.
     * @throws EntityNotFoundException quando nenhuma despesa for encontrada.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de despesas.
     */
    List<CestaItem> getDespesas(Integer codigoCesta, String token);
}
