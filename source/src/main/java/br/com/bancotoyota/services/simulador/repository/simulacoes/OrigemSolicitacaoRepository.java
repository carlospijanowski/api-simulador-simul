package br.com.bancotoyota.services.simulador.repository.simulacoes;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import br.com.bancotoyota.services.simulador.entities.OrigemSolicitacao;

@Repository
public interface OrigemSolicitacaoRepository extends MongoRepository<OrigemSolicitacao,String>{

}
