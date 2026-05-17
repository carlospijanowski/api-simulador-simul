package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Slf4j
public class Fluxo {
    @Schema(name = "parcelas", description = "Número da parcela", example = "1")
    @JsonProperty("parcelas")
    private Integer parcela;

    @Schema(name = "data-vencimento", description = "Data de vencimento da parcela do financiamento", example = "27/01/2020")
    @JsonProperty("data-vencimento")
    private String dataParcela;

    @Schema(name = "valor-parcela", description = "Valor da parcela", example = "240")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @Schema(name = "valor-intermediaria", description = "Valor da parcela intermediaria", example = "18000")
    @JsonProperty("valor-intermediaria")
    private BigDecimal valorIntermediaria;

    public Fluxo(Integer parcela, String dataParcela, BigDecimal valorParcela, BigDecimal valorIntermediaria) {
        this.parcela = parcela;
        this.dataParcela = dataParcela;
        this.valorParcela = valorParcela;
        this.valorIntermediaria = obterValorIntermediariaAjustado(valorIntermediaria);
    }

    private BigDecimal obterValorIntermediariaAjustado(BigDecimal valorIntermediaria) {
        /*
         * 20251007 - Fabio Silva | Luiz Bento - Conforme conversado com o Adriano Leal, diferenças nas parcelas deve aplicar
         * o arredondamento para baixo para não prejudicar o cliente
         * 20251007 - No momento, esta função está recebendo a diferença do valor da PMT do fluxo da operação calculado
         * pela planilha e o valor da parcela do financiamento, como no momento não recebemos as informações das parcelas
         * intermediárias utilizadas na simulação estamos inferindo que se o valor da diferença for menor que 1 real
         * a diferença é causada pelo arredondamento e estamos descartando o valor
         */
        if (valorIntermediaria.compareTo(BigDecimal.ONE) < 0) {
            log.info("Valor da intemerdiária cálculado na diferença entre o pmt e valor parcela de {}", valorIntermediaria);
            return BigDecimal.ZERO;
        } else {
            return valorIntermediaria;
        }
    }
}
