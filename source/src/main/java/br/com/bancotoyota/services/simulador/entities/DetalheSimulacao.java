package br.com.bancotoyota.services.simulador.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class DetalheSimulacao{

    @Schema(name = "data-primeiro-vencimento", description = "Data primeiro vencimento do financiamento", example = "20250201")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-primeiro-vencimento")
    private LocalDate dataPrimeiroVencimento;

    @Schema(name = "modalidade-id", description = "Codigo da modalidade do produto", example = "cdc")
	@JsonProperty("modalidade-id")
	private String modalidadeId;

    @Schema(name = "situacao-veiculo", description = "Estado de conservação do bem.", example = "0km")
	@JsonProperty("situacao-veiculo")
	private String situacaoVeiculo;

    @Schema(name = "taxa-subsidio", description = "Taxa do subsidio para financiamento", example = "1.02")
    @JsonProperty("taxa-subsidio")
    private BigDecimal taxaSubsidio;

    @Schema(name = "tipo-pessoa", description = "Tipo de pessoa", example = "pessoa-fisica")
    @JsonProperty("tipo-pessoa")
    private String tipoPessoa;

    @Schema(name = "uf-emplacamento", description = "UF do endereço de emplacamento do bem", example = "SP")
    @JsonProperty("uf-emplacamento")
    private String ufEmplacamento;

    @Schema(name = "valor-entrada", description = "Valor de entrada no financiamento", example = "27000")
    @JsonProperty("valor-entrada")
    private BigDecimal valorEntrada;

    @Schema(name = "valor-parcela-desejada", description = "Valor da parcela desejado no financiamento.", example = "1234")
    @JsonProperty("valor-parcela-desejada")
    private BigDecimal valorParcelaDesejada;

    @Schema(name = "valor-parcela-residual", description = "Valor da parcela residual", example = "18000")
    @JsonProperty("valor-parcela-residual")
    private BigDecimal valorParcelaResidual;

    @Schema(name = "valor-subsidio", description = "Valor total do subsidio", example = "5000")
    @JsonProperty("valor-subsidio")
    private BigDecimal valorSubsidio;

    @Schema(name = "seguro-auto", description = "Valor do seguro auto do bem a ser financiado",example = "3510.12" )
    @JsonProperty("seguro-auto")
    private BigDecimal seguroAuto;

    @Schema(name = "codigo-retorno", description = "Código da simulação (Código de Retorno)", example = "1")
	@JsonProperty("codigo-retorno")
	private String codigoRetorno;

    @Schema(name = "ano-modelo", description = "Ano do modelo do veículo", example = "2024")
    @JsonProperty("ano-modelo")
	private Integer anoModelo;	

} 