package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.DataBA;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.repository.propostas.DataBARepository;
import br.com.bancotoyota.services.simulador.services.DataCarencia;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;


import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@Slf4j
public class DataCarenciaImpl implements DataCarencia {

	public static final String DATA_PRIMEIRO_VENCIMENTO = "data-primeiro-vencimento";
	private DataBARepository repository;
	private RedisRepository redisRepository;

	@Autowired
	public DataCarenciaImpl(DataBARepository repository, RedisRepository redisRepository) {
		this.repository = repository;
		this.redisRepository = redisRepository;
	}

	@Override
	public boolean isDentroCarencia(SimulacaoRequest request, LocalDate dataCalculo) {
		log.info("isDentroCarencia() - Verificacao");
		if(isPlanoSemCarencia(request)) {
			return true;
		}

		LocalDate now = dataCalculo;
		LocalDate dataMaxima = now.plusDays(request.getCarenciaDoPlano().getMaximo() + 1L);
		LocalDate dataMinima = now.plusDays(request.getCarenciaDoPlano().getMinimo() - 1L);
		LocalDate dataPrimeiroVencimento = request.getDataPrimeiroVencimento(now);

		log.debug("isDentroCarencia() - Variaveis de verificacoes - dataCalculo: " + now + " - carenciaDataMinima: " + dataMinima + " - carenciaDataMaxima: " + dataMaxima + " - dataPrimeiroVencimento: " + dataPrimeiroVencimento);
		if (dataPrimeiroVencimento == null) {
			log.debug("isDentroCarencia() - Data do primeiro vencimento nao informada.");
			throw new BusinessValidationException(DATA_PRIMEIRO_VENCIMENTO, null,
					"Data do primeiro vencimento não informada.");
		}

		if (dataPrimeiroVencimento.isBefore(now)) {
			log.debug("isDentroCarencia() - Data do primeiro vencimento não pode ser no passado (data de cálculo " + now + ").");
			throw new BusinessValidationException(DATA_PRIMEIRO_VENCIMENTO, null,
					"Data do primeiro vencimento não pode ser no passado (data de cálculo " + now + ").");
		}

		if (dataPrimeiroVencimento.isAfter(dataMinima) && dataPrimeiroVencimento.isBefore(dataMaxima)) {
			return true;
		}

		log.debug("isDentroCarencia() - A data do primeiro vencimento esta invalida. Por favor, selecione uma nova data.");
		throw new BusinessValidationException("A data do primeiro vencimento est\u00e1 inv\u00e1lida. Por favor, selecione uma nova data.");
	}

	private boolean isPlanoSemCarencia(SimulacaoRequest request) {
		return request.getCarenciaDoPlano().getMinimo().equals(Constants.ZERO_INTEIRO) 
				&& request.getCarenciaDoPlano().getMaximo().equals(Constants.ZERO_INTEIRO);
	}

	@Override
	public LocalDate getDataCalculo() {
		DataBA dataBA = repository.findAll().stream().findFirst()
				.orElseThrow(() -> new IllegalStateException("DataBA não cadastrada no banco de dados do NewFront"));
		ControlesBA controlesBA;
		try {
			controlesBA = this.redisRepository.getDataBA();
		} catch (SerializationException ex) {
			log.error("Erro ao realizar serialização",ex);
			controlesBA = new ControlesBA();
		}

		if (dataBA.getAtual() != null || controlesBA.getDataAtualBA() != null) {
			return (dataBA.getAtual() != null && (controlesBA.getDataAtualBA() == null
					|| dataBA.getAtual().isAfter(controlesBA.getDataAtualBA().toLocalDate()))) ? dataBA.getAtual()
							: controlesBA.getDataAtualBA().toLocalDate();

		} else {
			// pega a data de hoje para usar de Data do BA , considerando que não tem data
			// do BA no redis e no Mongo.
			LocalDate dataBAAlternativa = LocalDate.now();
			while (dataBAAlternativa.getDayOfWeek() == DayOfWeek.SATURDAY
					|| dataBAAlternativa.getDayOfWeek() == DayOfWeek.SUNDAY) {
				dataBAAlternativa = dataBAAlternativa.plusDays(Constants.UM);
			}
			return dataBAAlternativa;
		}
	}

	@Override
	public void setDataCalculo(LocalDate novaData) {
		DataBA dataBA = repository.findAll().stream().findFirst().orElse(new DataBA(novaData));
		dataBA.setAtual(novaData);
		repository.save(dataBA);
	}
}
