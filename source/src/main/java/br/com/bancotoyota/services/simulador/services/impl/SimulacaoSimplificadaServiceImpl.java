package br.com.bancotoyota.services.simulador.services.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoSimplificadaResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.PrazoDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoSimplificadaResidualResponse;
import br.com.bancotoyota.services.simulador.entities.ClienteProspect;
import br.com.bancotoyota.services.simulador.entities.OrigemSolicitacao;
import br.com.bancotoyota.services.simulador.entities.PrazoSimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.entities.Saida;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificadaSaida;
import br.com.bancotoyota.services.simulador.entities.TipoSimulacao;
import br.com.bancotoyota.services.simulador.repository.simulacoes.SimulacaoSimplificadaRepository;
import br.com.bancotoyota.services.simulador.services.OrigemSolicitacaoService;
import br.com.bancotoyota.services.simulador.services.SimulacaoSimplificadaService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SimulacaoSimplificadaServiceImpl implements SimulacaoSimplificadaService{

	private SimulacaoSimplificadaRepository repository;
	
	private static final String SIMULACAO_SIMPLIFICADA = "simulacao-simplificada";
	
	@Autowired
	@Qualifier("simulacoesParceirosMongoTemplate")
	private MongoTemplate mongoTemplate;
	
	private OrigemSolicitacaoService origemService;
	
	@Autowired
	public SimulacaoSimplificadaServiceImpl (SimulacaoSimplificadaRepository repository,MongoTemplate mongoTemplate, OrigemSolicitacaoService origemService) {
		this.repository = repository;
		this.mongoTemplate = mongoTemplate;
		this.origemService = origemService;
	}
	
	public String insertSimulacaoSimplificada(SimulacaoSimplificada simulacao){
		String codigoSimulacao = null;
		if ( simulacao.getOrigemSolicitacao() != null && !simulacao.getOrigemSolicitacao().isEmpty() 
				&& simulacao.getSessionId() != null && !simulacao.getSessionId().isEmpty()) {
			try {
				OrigemSolicitacao origemSolicitacao = origemService.findOrigemSolicitacao(simulacao.getOrigemSolicitacao());
				/*
				  Descrição Origem Solicitação, para quando for enviar para o Salesforce, enviar uma descrição identificável na tela
				*/
				log.info("Salvando simulacao simplificada no Banco de dados {}...", simulacao.getSessionId());
				simulacao.setDescricaoOrigemSolicitacao(origemSolicitacao.getDescricao() != null && !origemSolicitacao.getDescricao().isEmpty() ? origemSolicitacao.getDescricao() : origemSolicitacao.getOrigemSolicitacao());
				simulacao = repository.insert(simulacao);		
				codigoSimulacao = simulacao.getId();
				log.info("Simulacao simplificada inserida {}...", codigoSimulacao);
			} catch (EntityNotFoundException e) {
				//Para as origens de solicitação que não estão no banco de dados, não grava a simulação no banco de dados e retorna o resultado da simulação, sem o codigo de simulação
				return null;
			}
		}
		return codigoSimulacao;
	}
		
	@Override
	public SimulacaoSimplificada simplificadaDesejadaToSimulacaoSimplificada(SimulacaoSimplificada simulacaoSimplificada, SimulacaoRequest simulacaoRequest, SimulacaoSimplificadaDesejadaResponse responseDesejada, SimulacaoSimplificadaDesejadaRequest request) {
		
		simulacaoSimplificada.setDataCriacao(LocalDateTime.now());
		simulacaoSimplificada.setTipoSimulacao(TipoSimulacao.SIMPLIFICADA_DESEJADA);
		if (request.getOrigemSolicitacao() != null) {
			simulacaoSimplificada.setOrigemSolicitacao(request.getOrigemSolicitacao());
		}
		
		simulacaoSimplificada.setSessionId(request.getSessionId());
		
        
		simulacaoSimplificada.getEntrada().getDetalheSimulacao().setValorParcelaDesejada(request.getValorParcelaDesejada());		
		
		Saida saida = simulacaoSimplificada.getSaida();
		saida.setDataBase(simulacaoRequest.getDataCalculo());
		saida.setDiaPagamentoParcelas(simulacaoRequest.getDataPrimeiroVencimento());
		
		SimulacaoSimplificadaSaida simulacaoSaida = new SimulacaoSimplificadaSaida();
		simulacaoSaida.setIsentaIOF(simulacaoRequest.getIsentaIOF());

		simulacaoSaida.setPeriodicidade(simulacaoRequest.getPeriodicidade());
		simulacaoSaida.setValorBem(simulacaoRequest.getValorBem());
		simulacaoSaida.setValorCestaServicos(responseDesejada.getValorCestaServico());
		simulacaoSaida.setValorEntrada(simulacaoRequest.getValorEntrada());
		simulacaoSaida.setValorRegistroContrato(responseDesejada.getValorRegistroContrato());
		
		simulacaoSaida.setValorTarifaCadastro(responseDesejada.getValorTarifaCadastro());
		simulacaoSaida.setValorUfEmplacamento(simulacaoRequest.getValorUfEmplacamento());

		if (responseDesejada.getPrazos() != null) {
			List<PrazoSimulacaoSimplificada> prazos = new ArrayList<>();
			for (PrazoDesejadaResponse p : responseDesejada.getPrazos()) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					mapper.findAndRegisterModules();
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);					
					String json = mapper.writeValueAsString(p);
					PrazoSimulacaoSimplificada prazo =  mapper.readValue(json, PrazoSimulacaoSimplificada.class);
					prazos.add(prazo);
				} catch (IOException ex) {
					throw new RuntimeException("erro convertendo retorno", ex);
				}
			}
			simulacaoSaida.setPrazos(prazos);
		}
		

		saida.setSimulacao(simulacaoSaida);
		simulacaoSimplificada.setSaida(saida);
		
		return simulacaoSimplificada;
	}
	
	@Override
	public SimulacaoSimplificada simplificadaResidualToSimulacaoSimplificada(SimulacaoSimplificada simulacaoSimplificada, SimulacaoParcResidualRequest finalRequestDesejada, SimulacaoSimplificadaResidualResponse responseResidual, SimulacaoSimplificadaResidualRequest request) {

		simulacaoSimplificada.setDataCriacao(LocalDateTime.now());
		simulacaoSimplificada.setTipoSimulacao(TipoSimulacao.SIMPLIFICADA_RESIDUAL);
		
		
		if (request.getOrigemSolicitacao() != null) {
			simulacaoSimplificada.setOrigemSolicitacao(request.getOrigemSolicitacao());
		}
		simulacaoSimplificada.setSessionId(request.getSessionId());
		
		
		simulacaoSimplificada.getEntrada().getDetalheSimulacao().setValorParcelaResidual(request.getValorParcelaResidual());
		simulacaoSimplificada.getEntrada().getDetalheSimulacao().setValorSubsidio(request.getValorSubsidio());
		simulacaoSimplificada.getEntrada().setParcelas(request.getParcelas());		
		
		Saida saida = simulacaoSimplificada.getSaida();
		saida.setDataBase(finalRequestDesejada.getDataCalculo());
		saida.setDiaPagamentoParcelas(finalRequestDesejada.getDataPrimeiroVencimento());
		
		SimulacaoSimplificadaSaida simulacaoSaida = new SimulacaoSimplificadaSaida();
		simulacaoSaida.setIsentaIOF(finalRequestDesejada.getIsentaIOF());
		
		simulacaoSaida.setPeriodicidade(finalRequestDesejada.getPeriodicidade());
		simulacaoSaida.setValorBem(finalRequestDesejada.getValorBem());
		simulacaoSaida.setValorCestaServicos(responseResidual.getValorCestaServico());
		simulacaoSaida.setValorEntrada(finalRequestDesejada.getValorEntrada());
		simulacaoSaida.setValorRegistroContrato(responseResidual.getValorRegistroContrato());
		
		simulacaoSaida.setValorTarifaCadastro(responseResidual.getValorTarifaCadastro());
		simulacaoSaida.setValorUfEmplacamento(finalRequestDesejada.getValorUfEmplacamento());
				
		if (responseResidual.getPrazos() != null) {
			List<PrazoSimulacaoSimplificada> prazos = new ArrayList<>();
			for (PrazoResidualResponse p : responseResidual.getPrazos()) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					mapper.findAndRegisterModules();
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);					
					String json = mapper.writeValueAsString(p);
					PrazoSimulacaoSimplificada prazo =  mapper.readValue(json, PrazoSimulacaoSimplificada.class);
					prazos.add(prazo);
				} catch (IOException ex) {
					throw new RuntimeException("erro convertendo retorno", ex);
				}
			}
			simulacaoSaida.setPrazos(prazos);
		}
		
	    
		saida.setSimulacao(simulacaoSaida);
		simulacaoSimplificada.setSaida(saida);		
		
		return simulacaoSimplificada;
	}
	
	
	@Override
	public void updateDadosContatoBySessionId(String sessionId, String origemSolicitacao, ClienteProspect clienteProspect) {
		log.info("Atualizando dados de contato por sessionId {}...", clienteProspect.getCpfCnpj());
		Query query = new Query();
		query.addCriteria(Criteria.where("sessionId").is(sessionId)
				.andOperator(Criteria.where("origemSolicitacao").is(origemSolicitacao)));
		
		UpdateDefinition update = new Update()
				.set("entrada.cliente.celular", clienteProspect.getCelular())
				.set("entrada.cliente.cpfCnpj", clienteProspect.getCpfCnpj())
				.set("entrada.cliente.email", clienteProspect.getEmail())
				.set("entrada.cliente.nome", clienteProspect.getNome());
		
		UpdateResult result = mongoTemplate.updateMulti(query, update, SimulacaoSimplificada.class);
		if (result.getMatchedCount() == 0) {
			throw new EntityNotFoundException(SimulacaoSimplificada.class, SIMULACAO_SIMPLIFICADA);
		}
		log.info("Dados de contato por sessionId atualizados {}...", result.getMatchedCount());
		
		
	}
	
	@Override
	public void updateSimulacaoSelecionadaByCodigoSimulacao(String origemSolicitacao, ClienteProspect clienteProspect) {
		log.info("Atualizando dados de contato por codigoSimulacao {}...", clienteProspect.getCodigoSimulacao());
		Query query = new Query();
		query.addCriteria(Criteria.where("id").is(clienteProspect.getCodigoSimulacao())
				.andOperator(Criteria.where("origemSolicitacao").is(origemSolicitacao)));
		
		UpdateDefinition update = new Update()
				.set("entrada.cliente.codigoSimulacao", clienteProspect.getCodigoSimulacao())
				.set("entrada.cliente.prazoSelecionado", clienteProspect.getPrazoSelecionado())
				.set("entrada.cliente.idOfertaSalesforce", clienteProspect.getIdOfertaSalesforce());
		
		UpdateResult result = mongoTemplate.updateMulti(query, update, SimulacaoSimplificada.class);
		if (result.getMatchedCount() == 0) {
			throw new EntityNotFoundException(SimulacaoSimplificada.class, SIMULACAO_SIMPLIFICADA);
		}
		log.info("Dados de contato por codigoSimulacao atualizados...");	
		
		
	}	

	@Override
	public SimulacaoSimplificada findByCodigoSimulacao(String codigoSimulacao) {
		Query query = new Query();
		query.addCriteria(Criteria.where("id").is(codigoSimulacao));
		log.info("Buscando Simulacao {}", codigoSimulacao);
		
		SimulacaoSimplificada simulacaoSimplificada = mongoTemplate.findOne(query, SimulacaoSimplificada.class, mongoTemplate.getCollectionName(SimulacaoSimplificada.class));
		if (simulacaoSimplificada != null) {
			log.info("Simulacao encontrada {}", codigoSimulacao);
			return simulacaoSimplificada;
		}else {
			throw new EntityNotFoundException(SimulacaoSimplificada.class, SIMULACAO_SIMPLIFICADA);
		}
	}

	@Override
	public SimulacaoSimplificada findByCodigoSimulacaoSessionIdOrigem ( String codigoSimulacao, String sessionId, String origem ) {

		SimulacaoSimplificada simulacaoSimplificada = findByCodigoSimulacao(codigoSimulacao);
		if ( simulacaoSimplificada != null 
				&& simulacaoSimplificada.getSessionId() != null && simulacaoSimplificada.getSessionId().equals(sessionId) 
				&& simulacaoSimplificada.getOrigemSolicitacao() != null && simulacaoSimplificada.getOrigemSolicitacao().equals(origem) ) {
			return simulacaoSimplificada;
		}else {
			throw new EntityNotFoundException(SimulacaoSimplificada.class, SIMULACAO_SIMPLIFICADA);
		}
		
	}

	@Override
	public SimulacaoSimplificada findByCodigoSimulacaoSessionIdOrigemPrazo(String codigoSimulacao, String sessionId,
			String origem, Integer prazo) {
		SimulacaoSimplificada simulacaoSimplificada = findByCodigoSimulacaoSessionIdOrigem ( codigoSimulacao, sessionId, origem );
		if ( simulacaoSimplificada.getSaida().getSimulacao().getPrazos().stream().anyMatch(p -> p.getParcelas().equals(prazo)) ) {
			return simulacaoSimplificada;
		}else {
			throw new EntityNotFoundException(SimulacaoSimplificada.class, "prazo-simulacao-simplificada");
		}
	}
	
}
