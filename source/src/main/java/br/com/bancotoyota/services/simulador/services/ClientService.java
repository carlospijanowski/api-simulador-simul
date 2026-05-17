package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.DadosCliente;
import br.com.bancotoyota.services.simulador.entities.ValorComprometidoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.services.exceptions.ServicoForaException;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.HttpStatusCodeException;

public interface ClientService {

    /**
     * Obtem os valores comprometidos seguro prestamista do cliente.
     * @param cpfCnpj CPF/CNPJ do cliente.
     * @return os valores comprometidos seguro prestamista do cliente.
     * @throws HttpStatusCodeException quando ocorrer um erro na API de cliente.
     */
	@Retryable(maxAttemptsExpression ="#{@retryConfig.getNumeroRetentativas()}", value = ServicoForaException.class, backoff = @Backoff(delay = Constants.DELAY_RETENTATIVAS, multiplier = 1))
    ValorComprometidoSeguroPrestamista.ClienteComprometimentoSeguro getValoresComprometidos(String cpfCnpj);

    /**
     * Obtem os dados do cliente informado.
     * @param cpfCnpj CPF/CNPJ do cliente.
     * @return os dados do cliente informado.
     */
	@Retryable(maxAttemptsExpression ="#{@retryConfig.getNumeroRetentativas()}", value = ServicoForaException.class, backoff = @Backoff(delayExpression = "#{@retryConfig.getDelayRetentativas()}", multiplier = 1))
    DadosCliente getDadosCliente(String cpfCnpj) ;

    /**
     * Verifica se o cliente informado esta ativo.
     * @param cpfCnpj CPF/CNPJ do cliente.
     * @return verdadeiro caso o cliente estiver ativo, falso caso contrario.
     */
	@Retryable(maxAttemptsExpression ="#{@retryConfig.getNumeroRetentativas()}", value = ServicoForaException.class, backoff = @Backoff(delayExpression = "#{@retryConfig.getDelayRetentativas()}", multiplier = 1))
    boolean isAtivo(String cpfCnpj);
}
