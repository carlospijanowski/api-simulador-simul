package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SimulacaoSimplificadaResidualResponse extends SimulacaoParcResidualResponse {

    @Schema(name = "valor-registro-contrato", description = "Valor do registro de contrato", example = "233.0")
    @JsonProperty("valor-registro-contrato")
    private BigDecimal valorRegistroContrato;

    @Schema(name = "valor-cesta-servicos", description = "Valor total da cesta de serviços", example = "23.0")
    @JsonProperty("valor-cesta-servico")
    private BigDecimal valorCestaServico;

    @Schema(name = "valor-tarifa-cadastro", description = "Valor da Tarifa de Cadastro", example = "23.0")
    @JsonProperty("valor-tarifa-cadastro")
    private BigDecimal valorTarifaCadastro;

    @Schema(name = "codigo-simulacao", description = "Código da simulação", example = "1")
    @JsonProperty("codigo-simulacao")
    private String codigoSimulacao;
}
