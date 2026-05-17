package br.com.bancotoyota.services.simulador.validadores;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;
import br.com.bancotoyota.services.simulador.entities.EnumErroValidacao;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class ValidadorBalaoCDC extends ValidadorBalaoModalidade {

    private LocalDate primeiroPagamento;
    private Integer prazo;
    private BigDecimal valorBase;
    private BigDecimal parcelaMinimaBalao;
    private BigDecimal parcelaMaximaBalao;
    private boolean isIntermediariaZerada;

    public ValidadorBalaoCDC(SimulacaoParcResidualRequest request, List<Prazo> prazos) {
        super(request.getIntermediarias(), request.getValorParcelaResidual(), request.getControleBalao(), request.getPermiteBalao());
        this.primeiroPagamento = request.getDataPrimeiroVencimento();
        this.prazo = request.getParcelas() != null ? request.getParcelas()[0] - 1 : 0;
        this.valorBase = request.getValorBem().subtract(request.getValorEntrada());
        this.isIntermediariaZerada = request.isIntermediariaZerada();

        if (prazos.size() == 1) {
            this.parcelaMinimaBalao = prazos.get(0).getPercentualMinBalao();
            this.parcelaMaximaBalao = prazos.get(0).getPercentualMaxBalao();
        }
    }

    @Override
    public void validar() {
        if (this.valorParcelaResidual != null && (this.valorParcelaResidual.compareTo(BigDecimal.ZERO) > 0)) {
            throw new BusinessValidationException("Plano CDC não pode ser usado com parcela residual");
        }

        final boolean isIntermediariasVazia = CollectionUtils.isEmpty(intermediarias);
        final boolean isParcelasComBalao = !isIntermediariasVazia;

        validarControleBalao(isIntermediariasVazia, isParcelasComBalao);
        if (!this.isIntermediariaZerada && Objects.nonNull(parcelaMinimaBalao) && Objects.nonNull(parcelaMaximaBalao)) {
            validarLimiteIntermediaria(isIntermediariasVazia);
        }
    }

    private void validarControleBalao(boolean isIntermediariasVazia, boolean isParcelasComBalao) {
        if (controleBalao != null) {
            if (controleBalao.equals(EnumControleBalao.OBRIGATORIO_BALAO) && isIntermediariasVazia) {
                throw new BusinessValidationException("A tabela selecionada exige a inserção de intermediarias");
            }

            if (controleBalao.equals(EnumControleBalao.NAO_PERMITE_BALAO) && isParcelasComBalao) {
                throw new BusinessValidationException("A tabela selecionada não permite a inserção de intermediarias");
            }

            if (controleBalao.equals(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA)) {
                validarUltimaParcela(isIntermediariasVazia);
            }
        }
    }

    private void validarLimiteIntermediaria(boolean isIntermediariasVazia) {
        final BigDecimal valorMinimaParcelaBalao = this.valorBase.multiply(this.parcelaMinimaBalao).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);
        final BigDecimal valorMaximaParcelaBalao = this.valorBase.multiply(this.parcelaMaximaBalao).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);

        if (!isIntermediariasVazia) {
            BigDecimal parcelasBalao = this.intermediarias.stream()
                    .map(ParcelaIntermediaria::getValor)
                    .reduce(BigDecimal::add)
                    .orElse(BigDecimal.ZERO);

            validarLimiteIntermediaria(parcelasBalao, valorMaximaParcelaBalao, valorMinimaParcelaBalao);
        }
    }

    private void validarLimiteIntermediaria(BigDecimal valorParcela, BigDecimal valorMaximaParcelaBalao, BigDecimal valorMinimaParcelaBalao) {
        if (valorParcela.compareTo(valorMaximaParcelaBalao) > 0) {
            log.error("Valor parcela: " + valorParcela + " [max]=" + valorMaximaParcelaBalao);
            throw new BusinessValidationException(null, valorMaximaParcelaBalao.toString(),
                    String.format(Constants.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE, valorMaximaParcelaBalao), EnumErroValidacao.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE.getKey());
        }
        if (valorParcela.compareTo(valorMinimaParcelaBalao) < 0) {
            log.error("Valor parcela: " + valorParcela + " [min]=" + valorMinimaParcelaBalao);
            throw new BusinessValidationException(null, valorMinimaParcelaBalao.toString(),
                    String.format(Constants.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE, valorMinimaParcelaBalao), EnumErroValidacao.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE.getKey());
        }
    }

    private void validarUltimaParcela(boolean isIntermediariasVazia) {
        String mensageErro = "A configuração do plano exige um balão na última parcela";

        if (isIntermediariasVazia) {
            throw new BusinessValidationException(mensageErro);
        }

        ParcelaIntermediaria ultimaIntermediaria = null;
        Optional<ParcelaIntermediaria> optionalParcelaIntermediaria = intermediarias.stream()
                .max(Comparator.comparing(ParcelaIntermediaria::getVencimento));

        if (optionalParcelaIntermediaria.isPresent()) {
            ultimaIntermediaria = optionalParcelaIntermediaria.get();
        }

        LocalDate primeira = this.primeiroPagamento;
        LocalDate ultimaParcela = primeira.plusMonths(this.prazo);

        if (Objects.isNull(ultimaIntermediaria) || !ultimaParcela.isEqual(ultimaIntermediaria.getVencimento())) {
            throw new BusinessValidationException(mensageErro);
        }
    }
}
