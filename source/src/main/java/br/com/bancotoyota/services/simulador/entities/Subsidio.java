package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.response.ValorSubsidio;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Log4j2
public class Subsidio implements Serializable {

    @JsonProperty("tipo-subsidio")
    private EnumTipoSubsidio tipoSubsidio;

    @JsonProperty("taxa-banco")
    private BigDecimal taxaBanco;

    /**
     * Só para subsídio do tipo fixo.
     */
    @JsonProperty("percentual-revendedora")
    private BigDecimal fixoPercentualRevendedora;

    /**
     * Só para subsídio do tipo fixo.
     */
    @JsonProperty("percentual-montadora")
    private BigDecimal fixoPercentualMontadora;

    @JsonProperty("tipo-distribuicao")
    private EnumTipoDistribuicao tipoDistribuicao;

    /**
     * Distribuição VALOR_PERCENTUAL
     */
    @JsonProperty("valor-max-marca")
    private BigDecimal vpValorMaxMarca;

    /**
     * Distribuição VALOR_PERCENTUAL
     */
    @JsonProperty("percentual-distr-marca")
    private BigDecimal vpPercentualDistrMarca;

    /**
     * FAIXA_VALOR
     */
    private List<FaixaValor> faixasValor;

    public void addFaixaValor(Integer sequencial, EnumResponsabilidade responsabilidade, BigDecimal valorInicial, BigDecimal valorFinal) {
        if (faixasValor == null) {
            faixasValor = new ArrayList<>();
        }
        faixasValor.add(new FaixaValor(sequencial, responsabilidade, valorInicial, valorFinal));
    }

    public ValorSubsidio fazerDistribuicao(BigDecimal valorSubsidio) {

        if (valorSubsidio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Bug do subsídio negativo na planilha, use uma data de primeiro pagamento mais para frente");
        }

        if (tipoSubsidio == EnumTipoSubsidio.FIXO) {
            return new ValorSubsidio(valorSubsidio.multiply(fixoPercentualRevendedora).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP),
                    valorSubsidio.multiply(fixoPercentualMontadora).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP));
        } else {
            if (tipoDistribuicao == EnumTipoDistribuicao.VALOR_PERCENTUAL) {
                return distribuirPorPercentual(valorSubsidio);
            } else if (tipoDistribuicao == EnumTipoDistribuicao.FAIXA_VALOR) {
                return distribuirPorFaixas(valorSubsidio);
            }
        }
        throw new IllegalStateException("dados incompletos para fazer a distribuição do subsídio");
    }

    private ValorSubsidio distribuirPorPercentual(BigDecimal valorSubsidio) {

        BigDecimal montadora = valorSubsidio.multiply(vpPercentualDistrMarca).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);
        if (montadora.compareTo(vpValorMaxMarca) > 0) {
            montadora = vpValorMaxMarca.setScale(2, RoundingMode.HALF_UP);
        }

        return new ValorSubsidio(valorSubsidio.subtract(montadora), montadora);
    }

    // plano 2797
    private ValorSubsidio distribuirPorFaixas(BigDecimal valorSubsidio) {

        BigDecimal[] valores = new BigDecimal[2];
        for (int i = 0; i < valores.length; i++) valores[i] = new BigDecimal("0.00");
        BigDecimal valorComputado = new BigDecimal("0.00");

        for (FaixaValor faixa : faixasValor) {
            BigDecimal diferenca;
            if (faixa.getValorFinal().compareTo(valorSubsidio) > 0) {
                diferenca = valorSubsidio.subtract(valorComputado);
            } else {
                diferenca = faixa.getValorFinal().subtract(valorComputado);
            }
            valores[faixa.responsabilidade.getIndice()] =
                    valores[faixa.responsabilidade.getIndice()].add(diferenca);
            valorComputado = valorComputado.add(diferenca);
            int comparacao = valorComputado.compareTo(valorSubsidio);
            if (comparacao == 0) {
                break;
            }
        }

        if (valorComputado.compareTo(valorSubsidio) < 0) {
            String mensagem = "Atenção, o valor do subsídio encontrado foi de R$ " + valorSubsidio + ", e é maior que o permitido, de até R$ " + valorComputado + ". Por favor, informe novos valores de taxa ou valor.";
            log.error(mensagem);
            throw new BusinessValidationException(mensagem);
        }

        return new ValorSubsidio(valores[EnumResponsabilidade.DEALER.getIndice()], valores[EnumResponsabilidade.MONTADORA.getIndice()]);
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    public static class FaixaValor implements Serializable {
        private Integer sequencial;
        private EnumResponsabilidade responsabilidade;
        @JsonProperty("valor-inicial")
        private BigDecimal valorInicial;
        @JsonProperty("valor-final")
        private BigDecimal valorFinal;
    }
}
