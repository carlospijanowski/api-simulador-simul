package br.com.bancotoyota.services.simulador.validadores;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.isNull;

public class ValidadorBalaoCiclo extends ValidadorBalaoModalidade {

	public ValidadorBalaoCiclo(List<ParcelaIntermediaria> intermediarias, BigDecimal valorParcelaResidual,
			EnumControleBalao controleBalao, Boolean permiteBalao) {
		super(intermediarias, valorParcelaResidual, controleBalao, permiteBalao);
	}

	@Override
	public void validar() {
		this.permiteBalao = isNull(this.permiteBalao) ? false : this.permiteBalao;

		if (this.permiteBalao) {
			if (this.valorParcelaResidual == null && (this.controleBalao.equals(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA)
					||
					this.controleBalao.equals(EnumControleBalao.OBRIGATORIO_BALAO)
			)) {
				throw new BusinessValidationException("Plano ciclo-toyota requer parcela residual");
			}
		}
	}
}
