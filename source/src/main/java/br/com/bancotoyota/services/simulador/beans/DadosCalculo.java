package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DadosCalculo {
	private LocalDate dataCalculo;
	private IOF taxasIOF;
	private BigDecimal valorBaseParaCalculoDoSeguro;
	@Setter
	private Plano plano;

	/**
	 * Fixa o tipo de seguro, ignorando todas as regras. Usado para fazer o cálculo
	 * do valor do balão (residual ou intermediária) sem que a mudança no tipo de
	 * seguro atrapalhe.
	 */
	@Setter
	private TipoDeSeguroPrestamista tipoFixo;

	private BigDecimal valorSeguroPrestamista;

	private BigDecimal valorSeguroGarantia;

	private SeguroPrestamistaPlusResponse segurosPrestamistas;

	private CalculoSeguroMecanicaResponse seguroGarantia;

	public Seguro getSeguro(TipoDeSeguroPrestamista seguro) {
		return this.segurosPrestamistas.getSeguros()
				.stream().filter(p -> p.getTipo().equals(seguro))
				.findFirst()
				.orElse(null);
	}

	public DadosCalculo(LocalDate dataCalculo, IOF taxasIOF, Seguro spf, Seguro svp,
			BigDecimal valorBaseParaCalculoDoSeguro, TipoDeSeguroPrestamista tipoFixo,
			BigDecimal valorSeguroPrestamista, BigDecimal valorSeguroGarantia) {
		this.dataCalculo = dataCalculo;
		this.taxasIOF = taxasIOF;
		this.valorBaseParaCalculoDoSeguro = valorBaseParaCalculoDoSeguro;
		this.tipoFixo = tipoFixo;
		this.valorSeguroPrestamista = valorSeguroPrestamista;
		this.valorSeguroGarantia = valorSeguroGarantia;
	}

	public DadosCalculo(LocalDate dataCalculo, IOF taxasIOF, SeguroPrestamistaPlusResponse segurosPrestamistas,
			BigDecimal valorBaseParaCalculoDoSeguro, TipoDeSeguroPrestamista tipoFixo,
			BigDecimal valorSeguroPrestamista,  CalculoSeguroMecanicaResponse seguroGarantia) {
		this.dataCalculo = dataCalculo;
		this.taxasIOF = taxasIOF;
		this.valorBaseParaCalculoDoSeguro = valorBaseParaCalculoDoSeguro;
		this.tipoFixo = tipoFixo;
		this.seguroGarantia = seguroGarantia;
		this.valorSeguroPrestamista = valorSeguroPrestamista;
		this.segurosPrestamistas = segurosPrestamistas;
	}
}
