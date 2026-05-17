package br.com.bancotoyota.services.simulador.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ParametrosSeguroMecanica {

	@JsonProperty("seguro-mecanica-selecionado")
	private SeguroMecanicaSelecionado seguroMecanicaSelecionado;

	@Schema(name = "cnpj-origem-negocio", description = "CNPJ origem negocio", example = "91919940439101")
	@JsonProperty("cnpj-origem-negocio")
	private String cnpjOrigemNegocio;

	@Schema(name = "marca", description = "Marca do bem financiado", example = "TOYOTA")
	private String marca;

	@Schema(name = "modelo", description = "Modelo do bem financiado", example = "YARIS XL PLUS CON. SED. 1.5 FLEX 16V AUT")
	private String modelo;

	@Schema(name = "data-inicio-seguro-mecanica", description = "Data de inicio do seguro mecanica", example = "false")
	@JsonProperty("data-inicio-seguro-mecanica")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	private LocalDate dataInicioSeguroMecanica;

	@Schema(name = "possui-seguro-mecanica", description = "Indica se o cliente já possui seguro mecanica", example = "false")
	@JsonProperty("possui-seguro-mecanica")
	private Boolean possuiSeguroMecanica;

	@Schema(name = "ano-fabricacao", description = "Ano de fabricação do veículo", example = "2019")
	@JsonProperty("ano-fabricacao")
	private Integer anoFabricacao;

	@Schema(name = "ano-modelo-veiculo", description = "Ano modelo do bem financiado", example = "2024")
	@JsonProperty("ano-modelo-veiculo")
	private String anoModelo;

	@Schema(name = "codigo-fipe", description = "Codigo da FIBE do bem financiado", example = "0022071")
	@JsonProperty("codigo-fipe")
	private String codigoFipe;

	@Schema(name = "quilometragem", description = "Quilometragem do veículo", example = "12375.12")
	private String quilometragem;
}
