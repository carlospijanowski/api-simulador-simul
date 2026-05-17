package br.com.bancotoyota.services.simulador.beans.request;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextoAvisoLegalRequest {

	@Schema(name = "codigo-simulacao", description = "Código da simulação.", example = "6063241419f09e20490cafd7")
	@ApiModelProperty(required = true)
	@JsonProperty("codigo-simulacao")
	@NotNull
	private String codigoSimulacao;

	@Schema(name = "session-id", description = "Identificador da sessão do direct", example ="333133-35464-343uifadbe4d" )
	@ApiModelProperty(required = true)
	@JsonProperty("session-id")
	@NotNull
	private String sessionId;

	@Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação", example ="OBO" )
	@ApiModelProperty(required = true,notes = "ABRADIT, INSTITUCIONAL, LANDING_PAGE, TDB, OBO")
	@JsonProperty("origem-solicitacao")
	@NotNull
	private String origem;

	@Schema(name = "prazo", description = "Número de parcelas", example = "36")
	@ApiModelProperty(required = true)
	@NotNull
	private Integer prazo;

}
