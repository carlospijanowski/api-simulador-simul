package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClienteProspect{

	@Schema(name = "cpf-cnpj", description = "CPF ou CPNJ do cliente.", example = "91919940439101")
	@JsonProperty("cpf-cnpj")
	private String cpfCnpj;

	@Schema(name = "nome", description = "Nome do cliente", example = "TESTE TESTE")
	@JsonProperty("nome")
	private String nome;

	@Schema(name = "telefone", description = "Número do telefone do cliente", example = "11957910895")
	@JsonProperty("telefone")
	private String telefone;

	@Schema(name = "celular", description = "Número do celular do cliente", example = "11957910895")
	@JsonProperty("celular")
	private String celular;

	@Schema(name = "email", description = "E-mail do cliente", example = "teste@teste.com")
	@JsonProperty("email")
	private String email;

	@Schema(name = "codigo-contrato", description = "Código do contrato", example = "1231243")
	@JsonProperty("codigo-contrato")
	private String codigoContrato;

	@Schema(name = "prazo-selecionado", description = "Prazo selecionado no financiamento", example = "12")
	@JsonProperty("prazo-selecionado")
	private Integer prazoSelecionado;

	@Schema(name = "id-oferta-salesforce", description = "Identificador da oferta Salesforce.", example = "123")
	@JsonProperty("id-oferta-salesforce")
	private String idOfertaSalesforce;

	@Schema(name = "codigo-simulacao", description = "Código da simulação.", example = "6063241419f09e20490cafd7")
	@JsonProperty("codigo-simulacao")
	private String codigoSimulacao;		
	
}