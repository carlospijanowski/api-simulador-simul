package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.EnumTipoValidacao;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ValidacaoRequest {

    @NotNull
    private Modalidade modalidade;

    @JsonProperty("origem-negocio")
    @Valid
    @NotNull
    private OrigemNegocio origemNegocio;

    /**
     * Será null em caso de PJ pois para esse caso não temos seguro prestamista.
     */
    @JsonProperty("dados-prestamista")
    private DadosPrestamista dadosPrestamista;

    @JsonProperty("tipo-seguro-prestamista")
    private TipoDeSeguroPrestamista tipoDeSeguroPrestamista;

    @JsonProperty("valor-seguro-prestamista")
    private BigDecimal valorSeguroPrestamista;

    /*
     * Os próximos campos são usados para validar o plano de financiamento
     */

    @JsonProperty("codigo-plano")
    @NotNull
    private Integer codigoPlano;

    @NotNull
    private Integer prazo;

    @JsonProperty("valor-bem")
    @NotNull
    private BigDecimal valorBem;

    @JsonProperty("valor-entrada")
    @NotNull
    private BigDecimal valorEntrada;

    /**
     * O residual só é obrigatório para modalidade ciclo.
     */
    @JsonProperty("valor-residual")
    private BigDecimal valorResidual;

    private List<ParcelaIntermediaria> intermediarias;

    @NotNull
    private String retorno;

    /**
     * Na alteração da proposta não temos a taxa mensal. Se não for passada não será validado.
     * No objeto proposta a taxa mensal é obrigatória, então esse valor só não será passado se for uma chamada interna.
     */
    @JsonProperty("taxa-mensal")
    private BigDecimal taxaMensal;

    @JsonProperty("taxa-subsidio")
    private BigDecimal taxaSubsidio;
    
    @JsonProperty("valor-subsidio")
    private BigDecimal valorSubsidio;
    
    @JsonProperty("valor-max-subsidio")
    private BigDecimal valorMaximoSubsidio;
    
    @JsonProperty("tipo-validacao")
    private EnumTipoValidacao tipoValidacao;
}
