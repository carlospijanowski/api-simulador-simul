package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;

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
public class Veiculo{

	@Schema(name = "ano-fabricacao", description = "Ano fabricação do véiculo", example = "2021")
	@JsonProperty("ano-fabricacao")
	private String anoFabricacao;

	@Schema(name = "ano-modelo", description = "Ano do modelo do véiculo", example = "2023")
	@JsonProperty("ano-modelo")
	private String anoModelo;

	@Schema(name = "codigo-fipe", description = "Código da FIPE", example = "002204-7")
	@JsonProperty("codigo-fipe")
	private String codigoFipe;

	@Schema(name = "marca-id", description = "Identificador da marco do veículo", example = "002")
	@JsonProperty("marca-id")
	private String marcaId;

	@Schema(name = "marca-descricao", description = "Nome da marca do veículo", example = "TOYOTA.")
	@JsonProperty("marca-descricao")
	private String marcaDescricao;

	@Schema(name = "modelo-id", description = "Identificador da marca do veículo", example = "002")
	@JsonProperty("modelo-id")
	private String modeloId;

	@Schema(name = "modelo-descricao", description = "Nome da marca do veículo", example = "COROLLA ALTIS 2.0 FLEX 16V AUT.")
	@JsonProperty("modelo-descricao")
	private String modeloDescricao;

	@Schema(name = "codigo-molicar", description = "Codigo da tabela FIPÉ do veículo.", example = "002112-1")
	@JsonProperty("codigo-molicar")
	private String codigoMolicar;

	@Schema(name = "valor-bem", description = "Valor do veículo", example = "90000")
	@JsonProperty("valor-bem")
	private BigDecimal valorBem;

}    
