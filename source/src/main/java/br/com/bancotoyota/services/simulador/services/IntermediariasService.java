package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.ResultadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class IntermediariasService {

	@Autowired
	private SimuladorParcResidualController residualService;

	@Autowired
	private PlanoService planoService;

	@Autowired
	private MotorTaxaPlanoService motorTaxaPlanoService;

	@Autowired
	private SimulacaoServices simulacaoServices;

	@Autowired
	private SeguroPrestamistaPlusServiceImpl prestamistaPlus;

	@Autowired
	private FeatureToggleConfig featureToggleConfig;

	/**
	 * Para usar a injeção via SpringBoot
	 */
	public IntermediariasService() {
	}

	/**
	 * Usado em testes unitários
	 */
	public IntermediariasService(SimuladorParcResidualController residualService, PlanoService planoService,
			SimulacaoServices simulacaoServices,SeguroPrestamistaPlusServiceImpl prestamistaPlus, FeatureToggleConfig featureToggleConfig) {
		this.residualService = residualService;
		this.planoService = planoService;
		this.simulacaoServices = simulacaoServices;
		this.prestamistaPlus = prestamistaPlus;
		this.featureToggleConfig = featureToggleConfig;
	}

	public List<ParcelaIntermediaria> criarIntermediarias(SimulacaoParcResidualRequest request,
			BigDecimal porcentualMaximoIntermediaria, BigDecimal toleranciaEmReais) {
		if (request.getModalidade() == Modalidade.CICLO_TOYOTA && (!featureToggleConfig.getBuscaApiMotorTaxaEnabled() || !featureToggleConfig.getCicloIntermediariasEnabled())) {
			throw new BusinessValidationException("intermediárias não podem ser usadas com plano ciclo-toyota");
		}
//		if (request.getModalidade() == Modalidade.CICLO_TOYOTA) {
//			throw new BusinessValidationException("intermediárias não podem ser usadas com plano ciclo-toyota");
//		}
		List<Prazo> prazos = null;
		if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
			prazos = motorTaxaPlanoService.findPrazos(request.getPlanoId(), null);
		} else {
			prazos = planoService.findPrazos(request.getPlanoId(), null);
		}

		// trabalharemos sempre com o maior prazo
		Prazo prazo = prazos.get(prazos.size() - 1);
		prazos.clear();
		prazos.add(prazo);
		request.setParcelas(new Integer[] { prazo.getNumeroDeParcelas() });

		LocalDate dataCalculo = residualService.getDataCalculo(request);

		request.setIntermediarias(criarIntermediariasImpl(request, dataCalculo));
		return encontrarValorDasIntermediarias(request, porcentualMaximoIntermediaria, toleranciaEmReais, dataCalculo,
				prazos, false);
	}

	List<ParcelaIntermediaria> criarIntermediariasImpl(SimulacaoParcResidualRequest request, LocalDate dataCalculo) {
		if (request.getPeriodicidade() != 1) {
			throw new BusinessValidationException("inserir intermediárias só deve ser usado com plano mensal");
		}
		int prazo = request.getParcelas()[0];
		int numeroIntermediarias = (prazo - 1) / 12;
		if (numeroIntermediarias > 3) {
			numeroIntermediarias = 3;
		}
		log.debug("prazo: " + prazo + " intermediárias: " + numeroIntermediarias);
		LocalDate primeira = request.getDataPrimeiroVencimento();
		if (request.getNDiasAposVencimento() != null && request.getNDiasAposVencimento()) {
			primeira = dataCalculo;
			primeira = primeira.plusMonths(1); // só tratamos planos mensais aqui
		}
		if (primeira == null) {
			throw new BusinessValidationException("data de pagamento não informada e nem vencimento-n-dias");
		}
		if (primeira.get(ChronoField.MONTH_OF_YEAR) > 6) {
			// se o primeiro pagamento já é em dezembro vamos colocar a primeira
			// intermediária para o ano seguinte
			primeira = primeira.plusYears(1);
		}
		primeira = primeira.with(ChronoField.MONTH_OF_YEAR, 12);
		List<ParcelaIntermediaria> list = new ArrayList<>(numeroIntermediarias);
		for (int i = 0; i < numeroIntermediarias; i++) {
			ParcelaIntermediaria parcelaIntermediaria = new ParcelaIntermediaria();
			LocalDate data = primeira.plusYears(i);
			parcelaIntermediaria.setVencimento(data);
			list.add(parcelaIntermediaria);
		}

		return list;
	}

	List<ParcelaIntermediaria> encontrarValorDasIntermediariasParaTeste(SimulacaoParcResidualRequest request,
			BigDecimal porcentualMaximoIntermediaria, BigDecimal tolerancaiEmReais, LocalDate dataCalculo) {
		return encontrarValorDasIntermediarias(request, porcentualMaximoIntermediaria, tolerancaiEmReais, dataCalculo,
				null, true);
	}

	/**
	 * O campo intermediarias do request deverá conter o número de intermediárias
	 * bem como suas datas. Essa rotina ira encontrar o valor das intermediárias
	 * para alcançar o valor desejado da parcela. O prazo deverá estar fixado e
	 * deverá ser o maior prazo para o plano.
	 * 
	 * @param porcentualMaximoIntermediaria porcentual em cima do valor financiado
	 */
	List<ParcelaIntermediaria> encontrarValorDasIntermediarias(SimulacaoParcResidualRequest request,
			BigDecimal porcentualMaximoIntermediaria, BigDecimal toleranciaEmReais, LocalDate dataCalculo,
			List<Prazo> prazos, boolean validarResultado) {

		if (request.getParcelas() == null || request.getParcelas().length != 1) {
			throw new IllegalStateException(
					"o cálculo do valor das intermediárias deve ser feito somente com o maior " + "prazo disponível");
		}

		if (prazos == null) {
			prazos = residualService.carregarPrazos(request);
		}

		BigDecimal valorAIntermediaria = BigDecimal.ZERO;
		ResultadoCalculadoParcelaResidual r = fazerSimulacao(valorAIntermediaria, request, dataCalculo, prazos, null);
		BigDecimal valorFinanciado = r.getValorTotalFinanciado();

		TipoDeSeguroPrestamista tipoA = r.getDadosSeguroPrestamista().getTipo();
		BigDecimal valorAParcela = r.getValorParcela();
		// valor máximo para a intermediária
		BigDecimal valorBIntermediaria = valorFinanciado.multiply(porcentualMaximoIntermediaria).divide(NumberUtils.CEM,
				2, RoundingMode.HALF_UP);

		// verificando se é possível atingir o valor desejado
		r = fazerSimulacao(valorBIntermediaria, request, dataCalculo, prazos, null);
		TipoDeSeguroPrestamista tipoB = r.getDadosSeguroPrestamista().getTipo();
		// menor parcela
		BigDecimal valorBParcela = r.getValorParcela();

		BigDecimal desejada = request.getValorParcelaDesejada();
		BigDecimal diferenca = valorBParcela.subtract(desejada);
		log.debug("valor máximo intermediaria: " + valorBIntermediaria + " menor valor da parcela: " + valorBParcela
				+ " valor parcela atual: " + valorAParcela + " valor desejado: " + desejada);
		if (diferenca.signum() >= 0) {
			if (diferenca.compareTo(toleranciaEmReais) > 0) {
				// Aqui temos a possibilidade de ainda tentar usar SVP ou NENHUM se o valor
				// atual é com SPF.
				// Para forçar não usar o SPF podemos setar a flag dizendo que ele foi removido.
				// Se trocar o seguro prestamista precisamos retornar esse informação para o
				// front.
				throw new EntityNotFoundException(
						new Erro("não é possível atingir o valor desejado da parcela mesmo com intermediárias"));
			}
			// nesse caso sabemos que o valor não foi atingido exatamente e estamos
			// trabalhando dentro da margem de
			// tolerância, por isso não cabe fazer o cálculo
			return retornarIntermediarias(request);
		}

		List<ParcelaIntermediaria> list = new ArrayList<>();
		// Se os tipos forem diferentes nenhum deles poderá ser NENHUM, pois as
		// condições que impedem o uso do seguro prestamista
		// não variam conforme valor da intermediária.
		// Como o SPF tem limitação de valor de parcela, sempre que os tipos forem
		// diferentes o B será SPF e o A será SVP
		if (tipoB == TipoDeSeguroPrestamista.SPF && tipoA != TipoDeSeguroPrestamista.SPF) {
			// não é necessário verificar se o SPF é disponível ou se foi removido, pois
			// caso contrário o tipo já não teria dado SPF
			Optional<Seguro> spfOptional = this.prestamistaPlus.getSeguroPrestamista(
					request.getParametrosDeSeguroPrestamista().getCnpjOrigemNegocio(), dataCalculo,
					TipoDeSeguroPrestamista.SPF);
			Seguro spf = null;
			if (spfOptional.isPresent()) {
				spf = spfOptional.get();
				if (spf.getMaximoDaParcela().compareTo(request.getValorParcelaDesejada()) >= 0) {
					list = usarTipoB(request, dataCalculo, prazos, valorAIntermediaria, valorBIntermediaria, tipoB,
							valorBParcela);
				} else {
					list = usarTipoA(request, dataCalculo, prazos, valorAIntermediaria, valorBIntermediaria, tipoA,
							valorAParcela);
				}
			}

		} else {
			list = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria, valorAParcela, valorBParcela,
					request, request.getValorParcelaDesejada());
		}

		if (validarResultado) {
			r = fazerSimulacao(list.get(0).getValor(), request, dataCalculo, prazos, null);
			r.setValorParcela(r.getValorParcela().setScale(2, RoundingMode.HALF_UP));
			if (r.getValorParcela().compareTo(request.getValorParcelaDesejada()) != 0) {
				throw new IllegalStateException("valor desejado: " + request.getValorParcelaDesejada()
						+ " - valor obtido: " + r.getValorParcela());
			}
		}

		return list;
	}

	private List<ParcelaIntermediaria> usarTipoB(SimulacaoParcResidualRequest request, LocalDate dataCalculo,
			List<Prazo> prazos, BigDecimal valorAIntermediaria, BigDecimal valorBIntermediaria,
			TipoDeSeguroPrestamista tipoB, BigDecimal valorBParcela) {
		ResultadoCalculadoParcelaResidual r = fazerSimulacao(valorAIntermediaria, request, dataCalculo, prazos, tipoB);
		// atualizando valor para fazer a proporção correta
		BigDecimal novoValorAParcela = r.getValorParcela();
		return calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria, novoValorAParcela, valorBParcela,
				request, request.getValorParcelaDesejada());
	}

	private List<ParcelaIntermediaria> usarTipoA(SimulacaoParcResidualRequest request, LocalDate dataCalculo,
			List<Prazo> prazos, BigDecimal valorAIntermediaria, BigDecimal valorBIntermediaria,
			TipoDeSeguroPrestamista tipoA, BigDecimal valorAParcela) {
		ResultadoCalculadoParcelaResidual r = fazerSimulacao(valorBIntermediaria, request, dataCalculo, prazos, tipoA);
		// atualizando valor para fazer a proporção correta
		BigDecimal novoValorBParcela = r.getValorParcela();
		return calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria, valorAParcela, novoValorBParcela,
				request, request.getValorParcelaDesejada());
	}

	private List<ParcelaIntermediaria> calcularValorDaIntermediaria(BigDecimal valorIntermediariaInicial,
			BigDecimal valorIntermediariaFinal, BigDecimal valorParcelaInicial, BigDecimal valorParcelaFinal,
			SimulacaoParcResidualRequest request, BigDecimal valorParcelaDesejada) {

		// vimos que a variação do valor da parcela é linear em relação ao valor da
		// intermediária, por isso vamos
		// determinar o valor candidato fazendo uma simples regra de três ao invés de
		// chutar no meio do intervalo
		BigDecimal diferencaParcela = valorParcelaInicial.subtract(valorParcelaFinal);
		BigDecimal diferencaIntermediaria = valorIntermediariaInicial.subtract(valorIntermediariaFinal);

		BigDecimal diferencaParcelaDesejada = valorParcelaInicial.subtract(valorParcelaDesejada);
		BigDecimal diferencaIntermediariaDesejada = diferencaParcelaDesejada.multiply(diferencaIntermediaria)
				.divide(diferencaParcela, 5, RoundingMode.HALF_UP);
		BigDecimal valorIntermediariaDesejada = valorIntermediariaInicial.subtract(diferencaIntermediariaDesejada);

		if (valorIntermediariaDesejada.signum() < 0) {
			throw new BusinessValidationException("O valor da parcela sem intermediárias já está abaixo do valor"
					+ " desejado. Não há necessidade de se adicionar intermediárias.");
		}

		List<ParcelaIntermediaria> intermediarias = request.getIntermediarias();
		intermediarias.forEach(p -> p.setValor(valorIntermediariaDesejada.setScale(2, RoundingMode.HALF_UP)));
		return retornarIntermediarias(request);
	}

	private List<ParcelaIntermediaria> retornarIntermediarias(SimulacaoParcResidualRequest request) {
		List<ParcelaIntermediaria> intermediarias = request.getIntermediarias();
		intermediarias.forEach(p -> p.setValor(p.getValor().setScale(2, RoundingMode.HALF_UP)));
		return intermediarias;
	}

	private ResultadoCalculadoParcelaResidual fazerSimulacao(BigDecimal valorIntermediaria,
			SimulacaoParcResidualRequest request, LocalDate dataCalculo, List<Prazo> prazos,
			TipoDeSeguroPrestamista tipoFixo) {

		if (valorIntermediaria.equals(BigDecimal.ZERO)) {
			request.setIntermediariaZerada(true);
		} else {
			request.setIntermediariaZerada(false);
		}

		List<ParcelaIntermediaria> intermediarias = request.getIntermediarias();
		intermediarias.forEach(p -> p.setValor(valorIntermediaria));
		ResultadoParcelaResidual response = residualService.fazerSimulacao(request, dataCalculo, prazos, tipoFixo);
		return response.getList().get(0);
	}
}
