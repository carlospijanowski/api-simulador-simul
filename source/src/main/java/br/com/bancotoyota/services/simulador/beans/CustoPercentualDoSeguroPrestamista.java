package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class CustoPercentualDoSeguroPrestamista implements Serializable {

	private final BigDecimal fator;

	private final TipoDeSeguroPrestamista tipo;

	/**
	 * Pode ser nulo caso o seguro não tenha nem sido considerado para ser usado por
	 * outro motivo que não o limite por CPF
	 */
	private Boolean limiteDeSPFExtrapolado;

	/**
	 * Pode ser nulo caso o seguro não tenha nem sido considerado para ser usado por
	 * outro motivo que não o limite por CPF
	 */
	private Boolean limiteDeSVPExtrapolado;

	private final Seguro dadosSeguro;

	private final Seguro ultimoSeguro;


	/**
	 * Verifica se os seguros já foram removidos e os limites para aplicar cada um
	 * 
	 * @param valorDaParcela Valor da parcela para verificação de qual seguro
	 *                       aplicar, pode ser nulo e nesse caso será
	 *                       desconsiderado. Isso é usado para o cálculo inicial.
	 *                       Depois do primeiro cálculo, já com o valor da parcela é
	 *                       feita nova validação.
	 * @return fator do seguro a ser aplicado para o cálculo do valor do seguro
	 */
	public static CustoPercentualDoSeguroPrestamista getFatorDoSeguro(DadosCalculo dadosCalculo,
			ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista, BigDecimal valorFinanciado,
			BigDecimal valorDaParcela, Integer idade, TipoPessoa tipoPessoa) {
		if (dadosCalculo.getTipoFixo() != null) {
			switch (dadosCalculo.getTipoFixo()) {
			case NENHUM:
				return retornarNenhum();
			case SPF: {
				Seguro seguro = dadosCalculo.getSeguro(TipoDeSeguroPrestamista.SPF);
				if (seguro == null) {
					return retornarNenhum();
				}
				return new CustoPercentualDoSeguroPrestamista(seguro.getPercentualDoSeguro(),
						TipoDeSeguroPrestamista.SPF, null, null, seguro, dadosCalculo.getSegurosPrestamistas().getUltimoSeguro());
			}
			case SVP: {
				Seguro seguro = dadosCalculo.getSeguro(TipoDeSeguroPrestamista.SVP);
				if (seguro == null) {
					return retornarNenhum();
				}

				return new CustoPercentualDoSeguroPrestamista(seguro.getPercentualDoSeguro(),
						TipoDeSeguroPrestamista.SVP, null, null, seguro, dadosCalculo.getSegurosPrestamistas().getUltimoSeguro());
			}

			}
		}

		if (tipoPessoa == TipoPessoa.PESSOA_JURIDICA) {
			// pessoa jurídica nunca tem seguro prestamista
			return retornarNenhum();
		}

		if (parametrosDeSeguroPrestamista == null) {
			throw new HttpMessageConversionException("parametros-seguro-prestamista não informado");
		}

		TipoDeSeguroPrestamista tipoEscolhido = parametrosDeSeguroPrestamista.getSeguroPrestamistaEscolhido();
		
		return getFatorDoSeguro(dadosCalculo.getSegurosPrestamistas(), parametrosDeSeguroPrestamista, valorFinanciado,
				valorDaParcela, idade, tipoEscolhido, tipoPessoa);
	}

	private static CustoPercentualDoSeguroPrestamista retornarNenhum() {
		return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM, null, null, null, null);
	}

	/**
	 * Verifica se os seguros já foram removidos e os limites para aplicar cada um
	 * 
	 * @param valorDaParcela Valor da parcela para verificação de qual seguro
	 *                       aplicar, pode ser nulo e nesse caso será
	 *                       desconsiderado. Isso é usado para o cálculo inicial.
	 *                       Depois do primeiro cálculo, já com o valor da parcela é
	 *                       feita nova validação.
	 * @param tipoEscolhido  Indica qual o tipo escolhido pelo usuário. Usado para
	 *                       verificar se o tipo escolhido pode ser usado. Se puder
	 *                       ser usado então ele será retornado. Caso não seja
	 *                       possível será retornado o tipo adequado. Pode ser null
	 *                       e nesse caso será retornado o tipo resultante de acordo
	 *                       com os outros parâmetros.
	 * @return fator do seguro a ser aplicado para o cálculo do valor do seguro
	 */
	private static CustoPercentualDoSeguroPrestamista getFatorDoSeguro(SeguroPrestamistaPlusResponse segurosPrestamista,
																	   ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista, BigDecimal valorFinanciado,
																	   BigDecimal valorDaParcela, Integer idade, TipoDeSeguroPrestamista tipoEscolhido, TipoPessoa tipoPessoa) {
		StatusSPF statusSPF = new StatusSPF();
		
		if(tipoEscolhido!=null && tipoEscolhido.equals(TipoDeSeguroPrestamista.NENHUM)) {
			return retornarNenhum(statusSPF);
		}
		
		List<Seguro> seguros = segurosPrestamista.getSeguros();

		if(parametrosDeSeguroPrestamista.getIdSeguroEscolhido()!=null) {
			Optional<Seguro> seguroEscolhido = seguros.stream().filter(p-> p.getItemId() == parametrosDeSeguroPrestamista.getIdSeguroEscolhido()).findFirst();
			if(seguroEscolhido.isPresent()) {
				Seguro seguro = seguroEscolhido.get();
				int posicao = seguros.indexOf(seguro);
				seguros.remove(posicao);
				seguros.add(Constants.ZERO_INTEIRO, seguro);
			}
		} else if (tipoEscolhido != null) {
			Optional<Seguro> seguroEscolhido = seguros.stream().filter(p-> p.getTipo().equals(tipoEscolhido)).findFirst();
			if(seguroEscolhido.isPresent()) {
				Seguro seguro = seguroEscolhido.get();
				seguros.remove(seguro);
				seguros.add(Constants.ZERO_INTEIRO, seguro);
			}
		}

		if (CollectionUtils.isEmpty(seguros)) {
			return retornarNenhum(statusSPF);
		}
		CustoPercentualDoSeguroPrestamista seguro = null;

		for (Seguro seguroEmAvaliacao : seguros) {
			if (seguroEmAvaliacao.getTipo().equals(TipoDeSeguroPrestamista.SPF)) {
				seguro = tentarSPF(seguroEmAvaliacao, parametrosDeSeguroPrestamista, valorFinanciado, valorDaParcela,
						statusSPF, idade, segurosPrestamista.getUltimoSeguro());
				if (seguro != null) {
					break;
				}
			} else if (seguroEmAvaliacao.getTipo().equals(TipoDeSeguroPrestamista.SVP) && !parametrosDeSeguroPrestamista.isSvpRemovido()) {
				seguro = tentarSVP(seguroEmAvaliacao, parametrosDeSeguroPrestamista, valorFinanciado,
						statusSPF.spfRemovidoRealmente, idade, segurosPrestamista.getUltimoSeguro());

				if (Optional.ofNullable(seguro)
						.map(s -> s.tipo)
						.filter(tipo -> tipo != TipoDeSeguroPrestamista.NENHUM)
						.isPresent()) {

					seguro.setLimiteDeSPFExtrapolado(statusSPF.spfExtrapolado);
					break;
				}

			}

		}

		if (Optional.ofNullable(seguro)
				.map(s -> s.tipo)
				.filter(tipo -> tipo != TipoDeSeguroPrestamista.NENHUM)
				.isPresent()) {

			return seguro;
		}

		return retornarNenhum(statusSPF);
	}

	private static CustoPercentualDoSeguroPrestamista retornarNenhum(StatusSPF statusSPF) {
		return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM,
				statusSPF.spfExtrapolado, null, null, null);
	}

	private static CustoPercentualDoSeguroPrestamista tentarSPF(Seguro spf, ParametrosDeSeguroPrestamista param,
			BigDecimal valorFinanciado, BigDecimal valorDaParcela, StatusSPF statusSPF, Integer idade, Seguro ultimoSeguro) {
		if (!param.isSpfDisponivel()) {
			return null;
		}

		if (!BooleanUtils.isNotFalse(param.getPermiteSPFComBaseNumeroContratos())) {
			statusSPF.spfExtrapolado = true;
			return null;
		}

		if (idade != null && (spf.getIdadeMinima() > idade || spf.getIdadeMaxima() < idade)) {
			return null;
		}

		valorFinanciado = valorFinanciado.add(valorFinanciado.multiply(spf.getPercentualDoSeguro()));

		if (valorFinanciado.compareTo(spf.getMaximoDoContrato()) > 0
				|| !cabeNaParcela(valorDaParcela, spf.getMaximoDaParcela())) {
			statusSPF.spfExtrapolado = true;
			return null;
		}

		BigDecimal valorPorCliente = param.getSpfValorComprometido().add(valorFinanciado);
		if (param.getValorOriginalTotalFinanciado() != null) {
			valorPorCliente = valorPorCliente.subtract(param.getValorOriginalTotalFinanciado());
		}
		if (valorPorCliente.compareTo(spf.getMaximoPorCliente()) > 0) {
			statusSPF.spfExtrapolado = true;
			return null;
		}

		statusSPF.spfExtrapolado = false;
		if (!param.isSpfRemovido()) {
			return new CustoPercentualDoSeguroPrestamista(spf.getPercentualDoSeguro(), TipoDeSeguroPrestamista.SPF,
					false, null, spf, ultimoSeguro);
		} else {
			statusSPF.spfRemovidoRealmente = true;
			return null;
		}
	}

	private static CustoPercentualDoSeguroPrestamista tentarSVP(Seguro svp, ParametrosDeSeguroPrestamista param,
			BigDecimal valorFinanciado, boolean spfRemovido, Integer idade, Seguro ultimoSeguro) {

		// Implementar regras de remoção de seguro SPF/SVP “em cascata”: incluir
		// automaticamente o SVP se o usuário
		// remover o SPF, de acordo com a configuração da loja.
		if ((!param.isSvpDisponivel() || (spfRemovido && !param.isSvpSeSpfRemovido()))) {
			return retornarNenhum();
		}

		if (!BooleanUtils.isNotFalse(param.getPermiteSVPComBaseNumeroContratos())) {
			return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM, null, true,null, null);
		}

		if (idade != null && (svp.getIdadeMinima() > idade || svp.getIdadeMaxima() < idade)) {
			return retornarNenhum();
		}

        if(idade == null && svp.getIdadeMinima() >= 70) {
            return retornarNenhum();
        }

		if (valorFinanciado.compareTo(svp.getMaximoDoContrato()) > 0) {
			return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM, null, true,null, null);
		}

		// Para o SVP o valor do máximo da parcela está com o valor zero e por isso não
		// vamos nem comparar o valor
		// calcular o valor do prestamista e inclui-lo na comparação (09/02/2021)
		valorFinanciado = valorFinanciado.add(valorFinanciado.multiply(svp.getPercentualDoSeguro()));

		BigDecimal valorPorCliente = param.getSvpValorComprometido().add(valorFinanciado);
		if (param.getValorOriginalTotalFinanciado() != null) {
			valorPorCliente = valorPorCliente.subtract(param.getValorOriginalTotalFinanciado());
		}

		if (valorPorCliente.compareTo(svp.getMaximoPorCliente()) <= 0) {
			return new CustoPercentualDoSeguroPrestamista(svp.getPercentualDoSeguro(), TipoDeSeguroPrestamista.SVP,
					null, false,svp, ultimoSeguro);
		}

		return new CustoPercentualDoSeguroPrestamista(BigDecimal.ZERO, TipoDeSeguroPrestamista.NENHUM, null, true,null, null);
	}

	private static boolean cabeNaParcela(BigDecimal valorDaParcela, BigDecimal maximo) {
		if (valorDaParcela == null) {
			return true;
		}
		return valorDaParcela.compareTo(maximo) <= 0;
	}
}
