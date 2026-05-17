package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.entities.Seguro;

@Service
public class ConversorComboPrestamistaParaSeguro {
	public List<Seguro> converter(Combo combo) {
		if (combo == null || CollectionUtils.isEmpty(combo.itens)) {
			return new ArrayList<Seguro>();
		}
		List<Seguro> listaSeguros = combo.itens.stream().map(p -> {
			Seguro seguro = new Seguro();
			seguro.setDescricao(p.getDescricao());
			seguro.setFatorDesemprego(p.getFatorDesemprego());
			seguro.setFatorInvalidez(p.getFatorInvalidez());
			seguro.setIdadeMaxima(p.getIdadeMaxima());
			seguro.setIdadeMinima(p.getIdadeMinima());
			seguro.setFatorMorte(p.getFatorMorte());
			seguro.setPercentualDoSeguro(BigDecimal.valueOf(p.getFatorPrestamista()));
			seguro.setFinalVigencia(p.getFinalVigencia());
			seguro.setFormaCalculoDescricao(p.getFormaCalculoDescricao());
			seguro.setFormaCalculoValor(p.getFormaCalculoValor());
			seguro.setIdadeMaxima(p.getIdadeMaxima());
			seguro.setIdadeMinima(p.getIdadeMinima());
			seguro.setInicioVigencia(p.getInicioVigencia());
			seguro.setItemId(p.getItemId());
			seguro.setOrdenacao(p.getOrdenacao());
			seguro.setPercentualIofSpf(p.getPercentualIofSpf());
			seguro.setPercentualProLabore(p.getPercentualProLabore());
			seguro.setPrazoFinal(p.getPrazoFinal());
			seguro.setPrazoInicio(p.getPrazoInicio());
			seguro.setTempoMaximoContrato(p.getTempoMaximoContrato());
			seguro.setTipo(TipoDeSeguroPrestamista.valueOf(p.getTipo()));
			seguro.setMaximoDaParcela(BigDecimal.valueOf(p.getValorLimiteParcela()));
			seguro.setMaximoDoContrato(BigDecimal.valueOf(p.getValorMaximoFinanciamentoContrato()));
			seguro.setMaximoPorCliente(BigDecimal.valueOf(p.getValorMaximoPorCliente()));
			return seguro;
		}).collect(Collectors.toList());

		return listaSeguros;
	}
}
