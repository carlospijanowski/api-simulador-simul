package br.com.bancotoyota.services.simulador.beans.prestamistaplus;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Iten {
	public int ordenacao;
	public String tipo;
	
	@JsonProperty("descricao")
	public String descricao;
	
	@JsonProperty("item-id")
	public int itemId;
	@JsonProperty("prazo-inicio")
	public int prazoInicio;
	@JsonProperty("prazo-final")
	public int prazoFinal;
	@JsonProperty("fator-prestamista")
	public double fatorPrestamista;
	@JsonProperty("forma-calculo-descricao")
	public String formaCalculoDescricao;
	@JsonProperty("forma-calculo-valor")
	public String formaCalculoValor;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	@JsonProperty("inicio-vigencia")
	private LocalDate inicioVigencia;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	@JsonProperty("final-vigencia")
	private LocalDate finalVigencia;

	@JsonProperty("tempo-maximo-contrato")
	public int tempoMaximoContrato;
	@JsonProperty("valor-limite-parcela")
	public double valorLimiteParcela;
	@JsonProperty("valor-maximo-financiamento-contrato")
	public double valorMaximoFinanciamentoContrato;
	@JsonProperty("percentual-iof-spf")
	public double percentualIofSpf;
	@JsonProperty("percentual-pro-labore")
	public double percentualProLabore;
	@JsonProperty("fator-morte")
	public double fatorMorte;
	@JsonProperty("fator-invalidez")
	public double fatorInvalidez;
	@JsonProperty("fator-desemprego")
	public double fatorDesemprego;
	@JsonProperty("idade-minima")
	public int idadeMinima;
	@JsonProperty("idade-maxima")
	public int idadeMaxima;
	@JsonProperty("valor-maximo-por-cliente")
	public double valorMaximoPorCliente;
}
