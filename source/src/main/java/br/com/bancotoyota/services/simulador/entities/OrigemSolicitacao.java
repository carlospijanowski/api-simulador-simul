package br.com.bancotoyota.services.simulador.entities;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Document(collection = "origens_solicitacao")
public class OrigemSolicitacao {
	
	@Id
	private String id;
	
	@JsonProperty("origem-solicitacao")
	private String origemSolicitacao;
	
	private String descricao;
	
	@JsonProperty("data-gravacao")
	private LocalDate dataGravacao;

}
