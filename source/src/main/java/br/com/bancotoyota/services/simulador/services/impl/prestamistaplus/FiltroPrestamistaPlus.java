package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class FiltroPrestamistaPlus {
	public SeguroPrestamistaPlusResponse executarFiltro(List<Seguro> segurosParaFiltrar, LocalDate dataBA,
			List<Integer> idsSegurosExcluidos, boolean removerTodosSPF, boolean removerTodosSVP) {
		// Garantir que está tudo ordenado
		// descartar o que não está vigente e o que já foi excluído
		Collections.sort(segurosParaFiltrar, Comparator.comparingInt(Seguro::getOrdenacao));
		Predicate<Seguro> filtroVigencia = p -> (dataBA.isAfter(p.getInicioVigencia())|| dataBA.isEqual(p.getInicioVigencia())) 
				&& (p.getFinalVigencia() == null || dataBA.isBefore(p.getFinalVigencia()) || dataBA.isEqual(p.getFinalVigencia()));

		Predicate<Seguro> filtroExcluidos = p -> idsSegurosExcluidos == null
				|| !idsSegurosExcluidos.contains(p.getItemId());

		Predicate<Seguro> filtroSemSPF = p -> !removerTodosSPF || (removerTodosSPF && !p.getTipo().equals(TipoDeSeguroPrestamista.SPF));
		Predicate<Seguro> filtroSemSVP = p -> !removerTodosSVP || (removerTodosSVP && !p.getTipo().equals(TipoDeSeguroPrestamista.SVP));
		List<Seguro> seguros = segurosParaFiltrar.stream()
				.filter(filtroVigencia)
				.filter(filtroSemSPF)
				.filter(filtroSemSVP)
				.collect(Collectors.toList());

		Seguro ultimoSeguro = null;
		List<Seguro> segurosFiltrados = new ArrayList<>();
		if (!seguros.isEmpty()) {
			int ultimoElemento = seguros.size() - 1;

			ultimoSeguro = seguros.get(ultimoElemento);
			segurosFiltrados = seguros.stream()
					.filter(filtroExcluidos)
					.collect(Collectors.toList());
		}

		return new SeguroPrestamistaPlusResponse(segurosFiltrados, ultimoSeguro);
	}
}
