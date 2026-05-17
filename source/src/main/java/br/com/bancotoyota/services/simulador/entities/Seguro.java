package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Seguro {

	public Seguro(TipoDeSeguroPrestamista tipo, BigDecimal maximoDaParcela, BigDecimal maximoDoContrato,
			BigDecimal maximoPorCliente, BigDecimal percentualDoSeguro, int idadeMinima, int idadeMaxima) {
		this.tipo = tipo;
		this.maximoDaParcela = maximoDaParcela;
		this.maximoDoContrato= maximoDoContrato;
		this.maximoPorCliente = maximoPorCliente;
		this.percentualDoSeguro = percentualDoSeguro;
		this.idadeMinima = idadeMinima;
		this.idadeMaxima = idadeMaxima;
	}

	@Schema(name = "tipo-seguro", description = "Tipo do seguro", example = "SPF")
	@JsonProperty("tipo-seguro")
	private TipoDeSeguroPrestamista tipo;

	@Schema(name = "limite-parcela", description = "Valor maximo da parcela do seguro", example = "3500")
	@JsonProperty("limite-parcela")
	private BigDecimal maximoDaParcela;

	@Schema(name = "valor-maximo-contrato", description = "Valor máximo do seguro", example = "200000")
	@JsonProperty("valor-maximo-contrato")
	private BigDecimal maximoDoContrato;

	@Schema(name = "valor-maximo-por-cliente", description = "Valor máximo de seguro por cliente", example = "600000")
	@JsonProperty("valor-maximo-por-cliente")
	private BigDecimal maximoPorCliente;


	@Schema(name = "percentual-seguro", description = "Percentual do valor do seguro no financiamento", example = "0.07")
	@JsonProperty("percentual-seguro")
	private BigDecimal percentualDoSeguro;

	@Schema(name = "idade-minima", description = "Idade minima permitida para contrato", example = "19")
	@JsonProperty("idade-minima")
	private int idadeMinima;

	@Schema(name = "idade-maxima", description = "Idade máxima permitida para contrato", example = "70")
	@JsonProperty("idade-maxima")
	private int idadeMaxima;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "descricao", description = "Nome do seguro", example = "SPF PLUS+")
	@JsonProperty("descricao")
	private String descricao;
	
	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "fator-desemprego", description = "Percentual utilizado para calcular o fator desemprego no financiamento", example = "0.0179")
	@JsonProperty("fator-desemprego")
	private double fatorDesemprego;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "fator-invalidez", description = "Percentual utilizado para calcular o fator invalidez no financiamento", example = "0.0019")
	@JsonProperty("fator-invalidez")
	private double fatorInvalidez;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "fator-morte", description = "Percentual utilizado para calcular o fator morte no financiamento", example = "0.0312")
	@JsonProperty("fator-morte")
	private double fatorMorte;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "final-vigencia", description = "Data final da vigência do seguro", example = "20221206")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	@JsonProperty("final-vigencia")
	private LocalDate finalVigencia;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "forma-calculo-descricao", description = "Forma de calculo do seguro", example = "Sobre Valor Financiado")
	@JsonProperty("forma-calculo-descricao")
	private String formaCalculoDescricao;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "forma-calculo-valor", description = "Valor que vai ser usado pra calculo seguro", example = "VLR_FINANCIADO")
	@JsonProperty("forma-calculo-valor")
	public String formaCalculoValor;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "inicio-vigencia", description = "Data inicial da vigência do seguro", example = "20211206")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	@JsonProperty("inicio-vigencia")
	private LocalDate inicioVigencia;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "item-id", description = "Identificador do item", example = "5")
	@JsonProperty("item-id")
	public int itemId;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "ordenacao", description = "Tipo de ordenação da lista", example = "1")
	@JsonProperty("ordenacao")
	private int ordenacao;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "percentual-iof-spf", description = "Percentual do IOF no seguro SPF", example = "0.38")
	@JsonProperty("percentual-iof-spf")
	private double percentualIofSpf;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "percentual-pro-labore", description = "Percentual do pro labore", example = "15")
	@JsonProperty("percentual-pro-labore")
	private double percentualProLabore;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "prazo-final", description = "Prazo final do seguro", example = "60")
	@JsonProperty("prazo-final")
	private int prazoFinal;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "prazo-inicio", description = "Prazo inicial do seguro", example = "1")
	@JsonProperty("prazo-inicio")
	private int prazoInicio;

	/*
	 * Usado no prestamista plus
	 */
	@Schema(name = "tempo-maximo-contrato", description = "Tempo maximo de contrato do seguro", example = "60")
	@JsonProperty("tempo-maximo-contrato")
	private int tempoMaximoContrato;
}
