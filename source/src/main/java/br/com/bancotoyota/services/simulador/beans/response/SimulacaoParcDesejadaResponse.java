package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaDesejada;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.entities.Subsidio;

import br.com.bancotoyota.services.simulador.utils.PrazoMaisProximoValorDesejadoUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
public class SimulacaoParcDesejadaResponse {
    private List<PrazoDesejadaResponse> prazos;

    @Schema(name = "data-base", description = "Data da base (BA)", example = "20241027")
    @JsonProperty("data-base")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate dataBase;

    /**
     * No caso de uma simulação de alternativa o request sempre é retornado.
     * Para outros casos esse campo é usado para facilitar a identificação de diferenças de valores que podem ocorrem
     * em caso de bugs e somente para simulações internas, que são aquelas que o serviço de proposta chama diretamente
     * quando vai abrir uma proposta ou pré-proposta para alteração.
     */
    private SimulacaoRequest request;

    @Schema(name = "valor-atingido", description = "Indica se o valor da parcela desejado foi atingido", example = "false")
    @JsonProperty("valor-atingido")
    private boolean valorAtingido;

    public SimulacaoParcDesejadaResponse() {
        super();
    }

    public SimulacaoParcDesejadaResponse(List<ResultadoCalculadoParcelaDesejada> resultados, Subsidio dadosDoSubsidio,
                                         LocalDate dataBase, SimulacaoParcDesejadaRequest request,
                                         BigDecimal limiteDiferencaEmReais, boolean retornarRequest) {
        this.dataBase = dataBase;
        if (retornarRequest) {
            this.request = request;
        }
        prazos = resultados.stream().map(r -> new PrazoDesejadaResponse(r, dadosDoSubsidio)).collect(Collectors.toList());
        this.valorAtingido = PrazoMaisProximoValorDesejadoUtils.selecionarPrazoMaisProximo(request, limiteDiferencaEmReais, prazos, this.dataBase);
    }
}
