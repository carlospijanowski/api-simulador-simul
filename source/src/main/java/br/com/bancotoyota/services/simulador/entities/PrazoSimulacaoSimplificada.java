package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.ValorSubsidio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PrazoSimulacaoSimplificada {

    @Schema(name = "parcelas", description = "Número de parcelas", example = "36")
    @JsonProperty("parcelas")
    private Integer parcelas;

    @Schema(name = "valor-parcela", description = "Valor da parcela", example = "240")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @Schema(name = "valor-parcela-residual", description = "Valor da parcela residual", example = "18000")
    @JsonProperty("valor-parcela-residual")
    private BigDecimal valorParcelaResidual;

    @Schema(name = "valor-iof", description = "Valor total do IOF do financiamento", example = "736.47")
    @JsonProperty("valor-iof")
    private BigDecimal valorIOF;

    @Schema(name = "valor-cet-anual", description = "Valor do custo efetivo anual do financiamento.", example = "23.02")
    @JsonProperty("valor-cet-anual")
    private BigDecimal taxaCetAnual;

    @Schema(name = "valor-taxa-mes", description = "Valor da taxa mensal do financiamento.", example = " 1.26")
    @JsonProperty("valor-taxa-mes")
    private BigDecimal taxaMensal;

    @Schema(name = "valor-taxa-anual", description = "Valor da taxa anual do financiamento.", example = " 16.21")
    @JsonProperty("valor-taxa-anual")
    private BigDecimal taxaAnual;

    @Schema(name = "valor-total-financiado", description = "Valor do total do financiamento", example = "130000")
    @JsonProperty("valor-total-financiado")
    private BigDecimal valorTotalFinanciado;

    @Schema(name = "valor-seguro-auto-parcela", description = "Valor da parcela do seguro auto", example = "200")
    @JsonProperty("valor-seguro-auto-parcela")
    private BigDecimal valorDoSeguroAutoNaParcela;

    @Schema(name = "valor-seguro-prestamista-parcela", description = "Valor da parcela do seguro prestamista", example = "340")
    @JsonProperty("valor-seguro-prestamista-parcela")
    private BigDecimal valorDoSeguroPrestamistaNaParcela;

    @Schema(name = "tipo-seguro-prestamista", description = "Tipo do seguro prestamista", example = "SPF")
    @JsonProperty("tipo-seguro-prestamista")
    private TipoDeSeguroPrestamista tipoDeSeguroPrestamista;

    @Schema(name = "valor-seguro-prestamista", description = "Valor do seguro prestamista", example = "4000")
    @JsonProperty("valor-seguro-prestamista")
    private BigDecimal valorDoSeguroPrestamista;

    @Schema(name = "svp-limite-extrapolado", description = "Indica se o valor do SVP foi ultrapassado.", example = "false")
    @JsonProperty("svp-limite-extrapolado")
    private Boolean limiteSVPExtrapolado;

    @Schema(name = "spf-limite-extrapolado", description = "Indica se o valor do SPF foi ultrapassado.", example = "false")
    @JsonProperty("spf-limite-extrapolado")
    private Boolean limiteSPFExtrapolado;

    @JsonProperty("itens-financiaveis")
    private List<ItemFinanciavel> itensFinanciaveisNaParcela;
    
    @JsonProperty("subsidio")
    private ValorSubsidio subsidio;

    @Schema(name = "valor-subsidio-usado", description = "Valor do subsidio calculada pela simulação", example = "345")
    @JsonProperty("valor-subsidio-usado")
    private BigDecimal valorSubsidioUsado;

    @Schema(name = "taxa-subsidio-usada", description = "Taxa de simulação calculada pela simulação", example = "23")
    @JsonProperty("taxa-subsidio-usada")
    private BigDecimal taxaSubsidioUsada;

    @Schema(name = "valor-total-prazo", description = "Valor original antes de alterar proposta", example = "100000")
    @JsonProperty("valor-total-prazo")
    private BigDecimal valorTotalPrazo;

    @JsonProperty("fluxo")
    private List<Fluxo> fluxo;

    @Schema(name = "porcentagem-parcela-residual", description = "Porcentagem do residual na parcela", example = "30")
    @JsonProperty("porcentagem-parcela-residual")
    private BigDecimal percentualParcelaResidual;

    @Schema(name = "porcentagem-entrada", description = "Porcentagem da entrada no financiamento", example = "30")
	@JsonProperty("porcentagem-entrada")
    private BigDecimal percentualEntrada;		
	
	
}
