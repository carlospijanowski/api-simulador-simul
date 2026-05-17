package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
public class SimulacaoSimplificadaSaida {

    @Schema(name = "isenta-iof", description = "Indica se o IOF está isento no financiamento", example = "N")
    @JsonProperty("isenta-iof")
    private String isentaIOF;

    @Schema(name = "periodicidade", description = "Numero da períodicidade da parcela", example = "1")
    @JsonProperty("periodicidade")
    private Integer periodicidade;

    @Schema(name = "porcentagem-residual", description = "Porcentagem do residual", example = "30")
    @JsonProperty("porcentagem-residual")
    private BigDecimal porcentagemResidual;

    @Schema(name = "porcentagem-entrada", description = "Porcentagem da entrada do financiamento", example = "30")
    @JsonProperty("porcentagem-entrada")
    private BigDecimal porcentagemEntrada;

    @Schema(name = "tipo", description = "Tipo da simulação", example = "SIMPLIFICADA-RESIDUAL")
    @JsonProperty("tipo")
    private String tipo;

    @Schema(name = "valor-bem", description = "Valor do veículo", example = "90000")
    @JsonProperty("valor-bem")
    private BigDecimal valorBem;

    @Schema(name = "valor-cesta-servicos", description = "Valor total da cesta de serviços", example = "23.0")
    @JsonProperty("valor-cesta-servicos")
    private BigDecimal valorCestaServicos;

    @Schema(name = "valor-entrada", description = "Valor de entrada no financiamento", example = "27000")
    @JsonProperty("valor-entrada")
    private BigDecimal valorEntrada;

    @Schema(name = "valor-registro-contrato", description = "Valor do registro de contrato", example = "233.0")
    @JsonProperty("valor-registro-contrato")
    private BigDecimal valorRegistroContrato;

    @Schema(name = "valor-tarifa-cadastro", description = "Valor da Tarifa de Cadastro", example = "23.0")
    @JsonProperty("valor-tarifa-cadastro")
    private BigDecimal valorTarifaCadastro;

    @Schema(name = "valor-uf-emplacamento", description = "Valor do registro de contrato para UF de emplacamento escolhida.")
    @JsonProperty("valor-uf-emplacamento")
    private BigDecimal valorUfEmplacamento;
    
    @JsonProperty("prazos")
    private List<PrazoSimulacaoSimplificada> prazos;
    
    
}
