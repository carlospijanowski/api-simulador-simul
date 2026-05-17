package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.entities.SeguroGarantia;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ParametrosSeguroGarantia {

	@Schema(name = "seguro-garantia-selecionado", description = "Dados do seguro garantia selecionado no financiamento")
	@JsonProperty("seguro-garantia-selecionado")
	private SeguroGarantiaSelecionado seguroGarantiaSelecionado;

	@Schema(name = "cnpj-origem-negocio", description = "CNPJ da seguradora", example = "91919940439101")
	@JsonProperty("cnpj-origem-negocio")
	private String cnpjOrigemNegocio;

	@Schema(name = "marca", description = "Marca do bem financiado", example = "TOYOTA")
	private String marca;

	@Schema(name = "modelo", description = "Modelo do bem financiado", example = "YARIS XL PLUS CON. SED. 1.5 FLEX 16V AUT")
	private String modelo;

	@Schema(name = "data-inicio-garantia", description = "Data de inicio do seguro garantia", example = "false")
	@JsonProperty("data-inicio-garantia")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	private LocalDate dataInicioGarantia;

	@Schema(name = "possui-garantia", description = "Indica se o cliente já possui garantia", example = "false")
	@JsonProperty("possui-garantia")
	private Boolean possuiGarantia;

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
