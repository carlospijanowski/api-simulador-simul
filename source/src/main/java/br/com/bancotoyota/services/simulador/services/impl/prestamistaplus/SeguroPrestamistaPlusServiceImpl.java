package br.com.bancotoyota.services.simulador.services.impl.prestamistaplus;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.prestamistaplus.Combo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.repository.SeguroPrestamistaPlusRepositoryImpl;
import br.com.bancotoyota.services.simulador.services.TokenService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SeguroPrestamistaPlusServiceImpl {
	private SeguroPrestamistaPlusRepositoryImpl repository;
	private ConversorComboPrestamistaParaSeguro conversorComboParaSeguro;
	private FiltroPrestamistaPlus filtroPrestamistaPlus;
	private boolean isHabilitado;
	private TokenService tokenService;

	@Autowired
	public SeguroPrestamistaPlusServiceImpl(SeguroPrestamistaPlusRepositoryImpl repository, ConversorComboPrestamistaParaSeguro conversor,FiltroPrestamistaPlus filtroPrestamistaPlus,
											@Value("${simulador.consulta.seguro.prestamista.enabled}") boolean isHabilitado,
											TokenService tokenService) {
		this.repository = repository;
		this.conversorComboParaSeguro = conversor;
		this.filtroPrestamistaPlus = filtroPrestamistaPlus;
		this.isHabilitado = isHabilitado;
		this.tokenService = tokenService;
	}

	public SeguroPrestamistaPlusResponse getSegurosPrestamista(String cnpjOrigemNegocio,
															   LocalDate dataBA, List<Integer> segurosExcluidos, boolean removerTodosSPF, boolean removerTodosSVP) {

		if (isHabilitado) {
			Combo conversor = this.repository.findSegurosDisponiveisPorOrigemNegocio(tokenService.generateDirectAccessToken().getToken(), cnpjOrigemNegocio);
			if (conversor.getDataInicio() != null && conversor.getDataInicio().isAfter(dataBA)) {
				return new SeguroPrestamistaPlusResponse();
			}
			List<Seguro> seguros = this.conversorComboParaSeguro.converter(conversor);
			return filtroPrestamistaPlus.executarFiltro(seguros, dataBA, segurosExcluidos, removerTodosSPF, removerTodosSVP);
		}

		return new SeguroPrestamistaPlusResponse();
	}
	
	public Optional<Seguro> getSeguroPrestamista(String cnpjOrigemNegocio , LocalDate dataBA, TipoDeSeguroPrestamista tipo) {
		if (isHabilitado) {
			SeguroPrestamistaPlusResponse segurosPrestamista = this.getSegurosPrestamista(cnpjOrigemNegocio, dataBA, null, false, false);
			return  segurosPrestamista.getSeguros().stream()
					.filter(p -> p.getTipo().equals(tipo))
					.findFirst();
		}

		return Optional.of(null);
	}

	public SeguroPrestamistaPlusResponse getSegurosPrestamistaByParametrizacao(LocalDate dataCalculo, ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista) {
		String cnpjOrigemNegocio = null;
		List<Integer> segurosPrestamistasExcluidos = null;
		boolean removerTodosSPF = false;
		boolean removerTodosSVP = false;

		if (ObjectUtils.isNotEmpty(parametrosDeSeguroPrestamista)) {
			cnpjOrigemNegocio = parametrosDeSeguroPrestamista.getCnpjOrigemNegocio();
			segurosPrestamistasExcluidos = parametrosDeSeguroPrestamista.getSegurosPrestamistasExcluidos();
			removerTodosSPF = parametrosDeSeguroPrestamista.isSpfRemovido();
			removerTodosSVP = parametrosDeSeguroPrestamista.isSvpRemovido();
		}

		return this.getSegurosPrestamista(cnpjOrigemNegocio,
				dataCalculo,
				segurosPrestamistasExcluidos,
				removerTodosSPF,
				removerTodosSVP);
	}
}
