package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.ResultadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.entities.EnumTipoDistribuicao;
import br.com.bancotoyota.services.simulador.entities.EnumTipoSubsidio;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
public class SimulacaoParcResidualResponse {
    private List<PrazoResidualResponse> prazos = new ArrayList<>();

    @Schema(name = "data-base", description = "Data da base (BA)", example = "20241027")
    @JsonProperty("data-base")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate dataBase;

    /**
     * Esse campo é usado para facilitar a identificação de diferenças de valores que podem ocorrem em caso de bugs e
     * somente para simulações internas, que são aquelas que o serviço de proposta chama diretamente quando vai
     * abrir uma proposta ou pré-proposta para alteração.
     */
    private SimulacaoRequest request;

    @Schema(name = "valor-atingido", description = "Indica se o valor da parcela desejado foi atingido", example = "false")
    @JsonProperty("valor-atingido")
    private boolean valorAtingido;

    public SimulacaoParcResidualResponse() {
        super();
    }

    /**
     * Cria a resposta do web service de simulações de um veículo e seus itens.
     */
    public SimulacaoParcResidualResponse(ResultadoParcelaResidual resultado, LocalDate dataBase,
                                         SimulacaoParcResidualRequest request, BigDecimal limiteDiferencaEmReais, boolean retornarRequest) {
        this.dataBase = dataBase;
        if (retornarRequest) {
            this.request = request;
        }

        // As checagens abaixo ocorrem somente quando o subsidio for tipo VARIAVEL, com distribuição por FAIXA
        if (resultado.getDadosDoSubsidio() != null &&
            resultado.getDadosDoSubsidio().getTipoSubsidio() != null && resultado.getDadosDoSubsidio().getTipoSubsidio().equals(EnumTipoSubsidio.VARIAVEL) &&
            resultado.getDadosDoSubsidio().getTipoSubsidio() != null && resultado.getDadosDoSubsidio().getTipoDistribuicao().equals(EnumTipoDistribuicao.FAIXA_VALOR) &&
            resultado.getDadosDoSubsidio().getFaixasValor() != null
            ) {
                ResultadoParcelaResidual prazosElegiveis = new ResultadoParcelaResidual(new ArrayList<ResultadoCalculadoParcelaResidual>(), resultado.getDadosDoSubsidio());

                Optional<Subsidio.FaixaValor> optionalMinValor = resultado.getDadosDoSubsidio().getFaixasValor().stream().min(Comparator.comparing(Subsidio.FaixaValor::getValorInicial));
                Optional<Subsidio.FaixaValor> optionalMaxValor = resultado.getDadosDoSubsidio().getFaixasValor().stream().max(Comparator.comparing(Subsidio.FaixaValor::getValorFinal));

                if (request.getValorSubsidio() != null) { // Subsídio por VALOR
                    for (ResultadoCalculadoParcelaResidual prazo : resultado.getList()) {
                        if (prazo.getValorSubsidio().compareTo(optionalMinValor.get().getValorInicial()) >= 0 &&
                            prazo.getValorSubsidio().compareTo(optionalMaxValor.get().getValorFinal()) <= 0) {
                                if (request.getValorSubsidio() != null &&
                                    request.getValorSubsidio().compareTo(prazo.getValorSubsidio()) <= 0) {
                                        prazosElegiveis.getList().add(prazo);
                                }
                        }
                    }
                } else { // Subsídio por TAXA
                    for (ResultadoCalculadoParcelaResidual prazo : resultado.getList()) {
                        if (prazo.getValorSubsidio().compareTo(optionalMinValor.get().getValorInicial()) >= 0 &&
                            prazo.getValorSubsidio().compareTo(optionalMaxValor.get().getValorFinal()) <= 0) {
                                prazosElegiveis.getList().add(prazo);
                        }
                    }
                }

                if (prazosElegiveis.getList().size() >= 1) {
                    prazos = prazosElegiveis.getList().stream().map(r -> new PrazoResidualResponse(r, prazosElegiveis.getDadosDoSubsidio())).collect(Collectors.toList());
                } else {
                    prazos = resultado.getList().stream().map(r -> new PrazoResidualResponse(r, resultado.getDadosDoSubsidio())).collect(Collectors.toList());
                }
        } else { // Fluxo normal caso não seja subsídio por faixa de valor, nao removendo ninguem da prazos.
            prazos = resultado.getList().stream().map(r -> new PrazoResidualResponse(r, resultado.getDadosDoSubsidio())).collect(Collectors.toList());
        }

        this.valorAtingido = PrazoMaisProximoValorDesejadoUtils.selecionarPrazoMaisProximo(request, limiteDiferencaEmReais, prazos, this.dataBase);
    }
}
