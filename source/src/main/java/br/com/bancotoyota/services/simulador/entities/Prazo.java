package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Luis Santos
 * Entidade que representa um prazo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Prazo implements Serializable {

    @Schema(name = "prazo", description = "Número de parcelas", example = "36")
    @JsonProperty("prazo")
    private Integer numeroDeParcelas;

    @Schema(name = "taxa-mes", description = "Usado caso queira usar uma taxa fixa e diferente da taxa do plano.", example = "1.26")
    @JsonProperty("taxa-mes")
    private BigDecimal taxaBanco;

    @Schema(name = "perc-min-balao", description = "Percentual mínimo do residual", example = "10.000000")
    @JsonProperty("perc-min-balao")
    private BigDecimal percentualMinBalao;

    @Schema(name = "perc-max-balao", description = "Percentual maximo do residual", example = "50.000000")
    @JsonProperty("perc-max-balao")
    private BigDecimal percentualMaxBalao;

    @Schema(name = "perc-min-entrada", description = "Percentual minimo de entrada do plano", example = "20.000000")
    @JsonProperty("perc-min-entrada")
    private BigDecimal percentualMinEntrada;

    @Schema(name = "perc-max-entrada", description = "Percentual maximo de entrada do plano", example = "50.000000")
    @JsonProperty("perc-max-entrada")
    private BigDecimal percentualMaxEntrada;

    @JsonProperty("dados-cobranca-servico-geolocalizacao")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizacao;

    /**
     * Em teoria isso não é necessário, mas vamos fazer isso para poder usar um plano CDC com o parcela desejada.
     * Esse não é um cenário real, mas vamos usar isso em testes até que a tela do CDC esteja pronta.
     */
    public BigDecimal getPercentualMinBalao() {
        if (percentualMinBalao == null) {
            return BigDecimal.ZERO;
        }
        return percentualMinBalao;
    }

    public BigDecimal getPercentualMaxBalao() {
        BigDecimal result = percentualMaxBalao;
        if (result == null) {
            result = BigDecimal.ZERO;
        }
        if (result.compareTo(BigDecimal.ZERO) == 0 && getPercentualMinBalao().compareTo(BigDecimal.ZERO) == 0) {
            result = NumberUtils.CEM;
        }
        return result;
    }
}
