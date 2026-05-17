package br.com.bancotoyota.services.simulador.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Esta classe recebe todas as flags referentes ao cálculo do seguro.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ParametrosDeSeguroPrestamista {

	public ParametrosDeSeguroPrestamista(boolean spfRemovido, boolean svpRemovido, boolean spfDisponivel,
			boolean svpDisponivel, boolean svpSeSpfRemovido, BigDecimal spfValorComprometido,
			BigDecimal svpValorComprometido, Boolean permiteSPFComBaseNumeroContratos,
			Boolean permiteSVPComBaseNumeroContratos, String cnpjOrigemNegocio,
			List<Integer> segurosPrestamistasExcluidos, Integer idSeguroEscolhido) {
		this(spfRemovido, svpRemovido, spfDisponivel, svpDisponivel, svpSeSpfRemovido, spfValorComprometido,
				svpValorComprometido, null, null, permiteSPFComBaseNumeroContratos, permiteSVPComBaseNumeroContratos,
				cnpjOrigemNegocio, segurosPrestamistasExcluidos, idSeguroEscolhido);
	}

	/**
	 * True quando o usuário removeu manualmente o SPF durante a simulação e portanto não deve ser recolocado automaticamente.
	 * Note que esse serviço não verifica se o usuário poderia ou não remover. Isso fica a cargo do front-end baseando-se
	 * nos serviços que carregam dados sobre a loja ou usuário.
	 * O SPF só é considerado removido somente se ele se enquadrar no limite do valor da parcela. Isso faz diferença
	 * no processamento do atributo {@link #svpSeSpfRemovido}.
	 */
	@Schema(name = "spf-removido", description = "Indica se a remover o seguro proteção financeira.", example = "false")
	@JsonProperty("spf-removido")
	private boolean spfRemovido;

	/**
	 * True quando o usuário removeu manualmente o SVP durante a simulação e portanto não deve ser recolocado automaticamente.
	 * Note que esse serviço não verifica se o usuário poderia ou não remover. Isso fica a cargo do front-end baseando-se
	 * nos serviços que carregam dados sobre a loja ou usuário.
	 */
	@Schema(name = "svp-removido", description = "Indica se a remover o seguro proteção financeira.", example = "false")
	@JsonProperty("svp-removido")
	private boolean svpRemovido;

	/**
	 * Se true spf pode ser usado.
	 */
	@Schema(name = "spf-disponivel", description = "Indica se está disponivel o seguro proteção financeira.", example = "false")
	@JsonProperty("spf-disponivel")
	private boolean spfDisponivel;

	/**
	 * Se true svp pode ser usado. Note que a preferência é sempre do SPF se
	 * possível.
	 */
	@Schema(name = "svp-disponivel", description = "Indica se está disponivel o seguro vida prestamista.", example = "false")
	@JsonProperty("svp-disponivel")
	private boolean svpDisponivel;

	/**
	 * Indica se o svp pode ser adicionado automaticamente caso o spf seja removido.
	 * Note que mesmo que a flag {@link #spfRemovido} seja true ainda é necessário
	 * verificar se o SPF poderia ser aplicado de acordo com o valor da parcela. Se
	 * não puder ser aplicado então nem é considerado removido.
	 */
	@Schema(name = "svp-se-spf-removido", description = "Indica se o svp pode ser adicionado automaticamente caso o spf seja removido.", example = "false")
	@JsonProperty("svp-se-spf-removido")
	private boolean svpSeSpfRemovido;

	@Schema(name = "spf-valor-comprometido", description = "Valor total do spf utilizado em contratos do cliente.", example = "580341.91")
	@JsonProperty("spf-valor-comprometido")
	@Nullable
	private BigDecimal spfValorComprometido;

	@Schema(name = "svp-valor-comprometido", description = "Valor total do svp utilizado em contratos do cliente.", example = "1092645.91")
	@JsonProperty("svp-valor-comprometido")
	@Nullable
	private BigDecimal svpValorComprometido;

	/**
	 * Caso se trate de uma alteração de proposta precisamos do valor original dessa
	 * proposta para descontar esse valor do total comprometido para o prestamista.
	 */
	@Schema(name = "valor-original-total-financiado", description = "Valor do financimento antes da alteração da propost.", example = "1092645.91")
	@JsonProperty("valor-original-total-financiado")
	private BigDecimal valorOriginalTotalFinanciado;

	/**
	 * Pode ser nulo. Nesse caso a simulação vai acontecer de forma normal,
	 * escolhendo o seguro prestamista de forma padrão. Caso seja especificado,
	 * então faz o cálculo com esse tipo de seguro, caso seja possível. A
	 * possibilidade por exemplo é definida pela loja, limite do proponente, idade,
	 * etc.
	 */
	@Schema(name = "seguro-prestamista-escolhido", description = "Seguro prestamista escolhido na simulação.", example = "SVP")
	@JsonProperty("seguro-prestamista-escolhido")
	private TipoDeSeguroPrestamista seguroPrestamistaEscolhido;

	// Foi criada uma regra no PBI
	// https://bancotoyota.atlassian.net/browse/POR-13232 para que no clientes ,
	// retorne um booleano em forma de 0 ou 1
	// para que possamos determinar se o cliente estourou o limite configurável de
	// três contratos por tipo de seguro. Temos isso para SPF e SVP
	@Schema(name = "permite-spf-com-base-numero-contratos", description = "Indica se o spf está disponivel para o cliente de acordo com o quantidade de contrato.", example = "false")
	@JsonProperty("permite-spf-com-base-numero-contratos")
	private Boolean permiteSPFComBaseNumeroContratos;

	@Schema(name = "permite-svp-com-base-numero-contratos", description = "Indica se o spf está disponivel para o cliente de acordo com o quantidade de contrato.", example = "false")
	@JsonProperty("permite-svp-com-base-numero-contratos")
	private Boolean permiteSVPComBaseNumeroContratos;

	public BigDecimal getSpfValorComprometido() {
		return spfValorComprometido == null ? BigDecimal.ZERO : spfValorComprometido;
	}

	public BigDecimal getSvpValorComprometido() {
		return svpValorComprometido == null ? BigDecimal.ZERO : svpValorComprometido;
	}

	@Schema(name = "cnpj-origem-negocio", description = "CPNJ da seguradora.", example = "33016221000107")
	@JsonProperty("cnpj-origem-negocio")
	private String cnpjOrigemNegocio;

	@Schema(name = "seguros-prestamistas-excluidos", description = "Seguros prestamista excluidos.", example = "[10, 12]")
	@JsonProperty("seguros-prestamistas-excluidos")
	private List<Integer> segurosPrestamistasExcluidos;
	
	/**
	 * Para casos de execução de simulação em cenário de edição de pré-proposta e proposta 
	 */
	@Schema(name = "id-seguro-escolhido", description = "Identificador do seguro escolhido.", example = "3")
	@JsonProperty("id-seguro-escolhido")
	private Integer idSeguroEscolhido;

}
