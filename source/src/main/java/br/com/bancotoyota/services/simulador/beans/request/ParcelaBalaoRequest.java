package br.com.bancotoyota.services.simulador.beans.request;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class ParcelaBalaoRequest {

	@Schema(name = "mes-ano-vencimento", description = "Mês e ano do vencimento do balão")
	@JsonProperty("mes-ano-vencimento")
	@NotBlank
	private String mesAnoVencimento;

	@Schema(name = "valor", description = "Valor da parcela", example = "234.23")
	@NotNull
	private BigDecimal valor;
	
}
