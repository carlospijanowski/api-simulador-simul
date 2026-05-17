package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Subsidio;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface SimulacaoServices {

    /**
     * Obtém a lista de prazos do plano escolhido.
     * @param planoId id do plano
     * @param percentualEntrada pode ser nulo e nesse caso retornará todos os prazos sem filtrar
     * @param prazos prazos a serem retornados, pode ser null ou vazio e desta forma todos serão retornados
     * @return Lista de prazos com as taxas. Se o plano não tiver um dado prazo a lista retornada pode ser menor
     *         que os prazos solicitados.
     * @throws BusinessValidationException caso nenhum prazo tenha sido encontrado de acordo com o #percentualEntrada e #prazos.
     * @throws EntityNotFoundException caso não exista registro para o plano
     */
    List<Prazo> getParcelas(Integer planoId, BigDecimal percentualEntrada, Collection<Integer> prazos,
                            BigDecimal taxaMes, Integer prazoDesejado);

    /**
     * Obtem as taxas IOF e a base calculo
     */
    TaxasIOF getTaxasIOF(TipoPessoa tipoPessoa, String isentaIof);

    Subsidio getSubsidio(Integer planoId);

    void validaRequestSimulacao(SimulacaoParcResidualRequest request);
}
