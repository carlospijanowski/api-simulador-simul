package br.com.bancotoyota.services.simulador.repository.propostas;

import br.com.bancotoyota.services.simulador.entities.DataBA;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataBARepository extends MongoRepository<DataBA, String> {
}
