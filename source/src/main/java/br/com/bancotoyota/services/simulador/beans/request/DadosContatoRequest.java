package br.com.bancotoyota.services.simulador.beans.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DadosContatoRequest {

	@Schema(name = "codigo-simulacao", description = "Código da simulação.", example = "674a55c2f4e9e5509f6cd5ba")
	@ApiModelProperty(required=true)
	@JsonProperty("codigo-simulacao")
	@NotBlank
	private String codigoSimulacao;

	@Schema(name = "id-oferta-salesforce", description = "Identificador da oferta Salesforce.", example = "123")
	@JsonProperty("id-oferta-salesforce")
	private String idOfertaSalesforce;

	@Schema(name = "prazo", description = "Número de parcelas", example = "36")
	@JsonProperty("prazo")
	private Integer prazo;

	@Schema(name = "cpf-cnpj", description = "CPF ou CPNJ do cliente.", example = "91919940439101")
	@ApiModelProperty(required=true)
	@JsonProperty("cpf-cnpj")
	@NotBlank
	private String cpfCnpj;

	@Schema(name = "nome", description = "Nome do cliente", example = "TESTE TESTE")
	@ApiModelProperty(required=true)
	@JsonProperty("nome")
	@NotBlank
	private String nome;

	@Schema(name = "telefone", description = "Número do telefone do cliente", example = "11957910895")
	@JsonProperty("telefone")
	private String telefone;

	@Schema(name = "celular", description = "Número do celular do cliente", example = "11957910895")
	@ApiModelProperty(required=true)
	@JsonProperty("celular")
	@NotBlank
	private String celular;

	@Schema(name = "email", description = "E-mail do cliente", example = "teste@teste.com")
	@ApiModelProperty(required=true)
	@JsonProperty("email")
	@NotBlank
	private String email;

	@Schema(name = "session-id", description = "Identificador da sessão do direct", example ="333133-35464-343uifadbe4d" )
	@ApiModelProperty(required=true)
	@JsonProperty("session-id")
	@NotBlank
	private String sessionId;

	@Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação", example ="OBO" )
	@ApiModelProperty(required=true,notes = "ABRADIT, INSTITUCIONAL, LANDING_PAGE, TDB, OBO")
	@JsonProperty("origem-solicitacao")
	@NotNull
	private String origem;
	
}
