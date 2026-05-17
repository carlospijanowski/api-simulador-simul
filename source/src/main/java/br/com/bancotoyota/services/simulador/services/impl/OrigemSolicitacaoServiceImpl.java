package br.com.bancotoyota.services.simulador.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.bancotoyota.services.simulador.entities.OrigemSolicitacao;
import br.com.bancotoyota.services.simulador.services.OrigemSolicitacaoService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrigemSolicitacaoServiceImpl implements OrigemSolicitacaoService{
	
	@Autowired
	@Qualifier("simulacoesParceirosMongoTemplate")
	private MongoTemplate mongoTemplate;
	
	@Autowired
	public OrigemSolicitacaoServiceImpl( MongoTemplate mongoTemplate ) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public OrigemSolicitacao findOrigemSolicitacao(String origemSolicitacao) {
		log.info("Buscando Origem Solicitacao {}", origemSolicitacao);
		Query query = new Query();
		query.addCriteria(Criteria.where("origemSolicitacao").is(origemSolicitacao));
		
		OrigemSolicitacao origemSolicitacaoSimulacao = mongoTemplate.findOne(query, OrigemSolicitacao.class, mongoTemplate.getCollectionName(OrigemSolicitacao.class));
		
		if (origemSolicitacaoSimulacao != null) {
			log.info("Origem de Solicitacao encontrada {}", origemSolicitacaoSimulacao.getDescricao());
			return origemSolicitacaoSimulacao;
		}else {
			throw new EntityNotFoundException(OrigemSolicitacao.class, "origem-solicitacao");
		}
	}
	
	

}
