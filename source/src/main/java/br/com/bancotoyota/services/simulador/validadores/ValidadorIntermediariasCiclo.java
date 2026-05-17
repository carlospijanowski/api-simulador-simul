package br.com.bancotoyota.services.simulador.validadores;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.RestricaoPercentualBalao;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;
import br.com.bancotoyota.services.simulador.entities.EnumErroValidacao;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ValidadorIntermediariasCiclo extends ValidadorBalaoModalidade {

    private LocalDate primeiroPagamento;
    private Integer prazo;
    private BigDecimal parcelaMinimaBalao;
    private BigDecimal parcelaMaximaBalao;
    private SimulacaoParcResidualRequest request;
    private LocalDate dataCalculo;


    public ValidadorIntermediariasCiclo(SimulacaoParcResidualRequest request, List<Prazo> prazos) {
        super(request.getIntermediarias(), request.getValorParcelaResidual(), request.getControleBalao(), request.getPermiteBalao());
        this.primeiroPagamento = request.getDataPrimeiroVencimento();
        this.prazo = request.getParcelas() != null && request.getParcelas().length > 0 ? request.getParcelas()[0] : 0;
        this.request = request;
        this.dataCalculo = request.getDataCalculo();

        if (this.prazo == 0) {
            log.error("A simulação requer um prazo.");
            throw new BusinessValidationException(null, "", "A simulação requer um prazo.", EnumErroValidacao.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE.getKey());
        }

        if (prazos.size() == 1) {
            this.parcelaMinimaBalao = prazos.get(0).getPercentualMinBalao();
            this.parcelaMaximaBalao = prazos.get(0).getPercentualMaxBalao();
        }
    }

    @Override
    public void validar() {

        validarControleBalao();
        validaRestricoesParcelasIntermediariasParaCiclo();
        validaQuatidadeIntermediarias();

    }

    private void validarControleBalao() {
        if (controleBalao != null) {
            if (controleBalao.equals(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA)) {
                validarUltimaParcela();
            }
            if (controleBalao.equals(EnumControleBalao.OBRIGATORIO_BALAO) || controleBalao.equals(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA)) {
                if (request.getIntermediarias() == null || (request.getIntermediarias() != null && request.getIntermediarias().size() == 0)) {
                    throw new BusinessValidationException("É obrigatório ao menos uma parcela intermediária.");
                }
            }
        }
    }

    private void validarLimiteIntermediaria(BigDecimal valorParcela, BigDecimal valorMaximaParcelaBalao, BigDecimal valorMinimaParcelaBalao, BigDecimal porcentagemMinima, BigDecimal porcentagemMaxima) {
        String exceptionMessage = "Valor da parcela intermediária excedeu o máximo permitido. Valor deverá estar entre " + porcentagemMinima.toString() + "%" + " e " + porcentagemMaxima.toString() + "%";

        if (valorParcela.compareTo(valorMaximaParcelaBalao) > 0) {
            log.error("Valor parcela: " + valorParcela + " [max]=" + valorMaximaParcelaBalao);
            throw new BusinessValidationException(null, valorMaximaParcelaBalao.toString(), exceptionMessage, EnumErroValidacao.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE.getKey());
        }
        if (valorParcela.compareTo(valorMinimaParcelaBalao) < 0) {
            log.error("Valor parcela: " + valorParcela + " [min]=" + valorMinimaParcelaBalao);
            throw new BusinessValidationException(null, valorMinimaParcelaBalao.toString(), exceptionMessage, EnumErroValidacao.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE.getKey());
        }
    }

    private void validarUltimaParcela() {
        String mensageErro = "Balão na última parcela obrigatório";

        ParcelaIntermediaria ultimaIntermediaria = null;
        Optional<ParcelaIntermediaria> optionalParcelaIntermediaria = intermediarias.stream().max(Comparator.comparing(ParcelaIntermediaria::getVencimento));

        if (optionalParcelaIntermediaria.isPresent()) {
            ultimaIntermediaria = optionalParcelaIntermediaria.get();

            if (Objects.nonNull(parcelaMinimaBalao) && Objects.nonNull(parcelaMaximaBalao)) {
                final BigDecimal valorMinimaParcelaBalao = this.request.getValorBem().multiply(this.parcelaMinimaBalao).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);
                final BigDecimal valorMaximaParcelaBalao = this.request.getValorBem().multiply(this.parcelaMaximaBalao).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);

                validarLimiteIntermediaria(ultimaIntermediaria.getValor(), valorMaximaParcelaBalao, valorMinimaParcelaBalao, this.parcelaMinimaBalao, this.parcelaMaximaBalao);

            }
        }

        if (Objects.isNull(this.primeiroPagamento) || (Objects.nonNull(this.request.getNDiasAposVencimento()) && this.request.getNDiasAposVencimento())) {
            this.primeiroPagamento = this.dataCalculo;
        }

        Integer ajusteQtdMeses = 0;
        if (Objects.isNull(this.request.getNDiasAposVencimento()) || !this.request.getNDiasAposVencimento()) {
            ajusteQtdMeses = 1;
        }

        LocalDate ultimaParcela = this.primeiroPagamento.plusMonths(this.prazo - ajusteQtdMeses);
        if (Objects.isNull(ultimaIntermediaria) || !YearMonth.from(ultimaParcela).equals(YearMonth.from(ultimaIntermediaria.getVencimento()))) {
            log.info("validarUltimaParcela() - Validação da data de pagamento contra a data do residual - dataUltimaParcela: {} - dataUltimaIntermediaria: {} - dataPrimeiroPagamento: {}", ultimaParcela, ultimaIntermediaria, this.primeiroPagamento);
            throw new BusinessValidationException(mensageErro);
        }
    }

    private void validaRestricoesParcelasIntermediariasParaCiclo() {
        if (request.getIntermediarias() != null && request.getIntermediarias().size() > 1) {

            long limiteMaximoIntervaloResidual = prazo - (request.getIntervaloResidual() == null ? 0 : request.getIntervaloResidual() + 1);

            List<ParcelaIntermediaria> grupoIntermediarias = new ArrayList<>();
            request.getIntermediarias().forEach(item -> {

                if (item.getVencimento() == null) {
                    throw new BusinessValidationException("Plano Ciclo vencimento da intermediária é obrigatório");
                }

                if (item.getValor() != null) {
                    grupoIntermediarias.add(new ParcelaIntermediaria(item.getValor(), primeiroPagamento.until(item.getVencimento(), ChronoUnit.MONTHS), getDataVencimento(item.getVencimento())));
                }
            });

            grupoIntermediarias.forEach(intermediaria -> intermediaria.setNumeroParcela(intermediaria.getNumeroParcela() + 1));

            request.setIntermediarias(request.getIntermediarias().stream().filter(fil -> fil.getValor() != null).collect(Collectors.toList()));

            if (request.getControleBalao() == EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA) {
                Collections.sort(grupoIntermediarias, Comparator.comparingLong(ParcelaIntermediaria::getNumeroParcela));
                ParcelaIntermediaria intermediariaResidual = grupoIntermediarias.get(grupoIntermediarias.size() - 1);
                grupoIntermediarias.remove(intermediariaResidual);

                if (intermediariaResidual.getNumeroParcela() > prazo) {
                    throw new BusinessValidationException(null, "", "Vencimento da ultima parcela não pode ser superior ao prazo", EnumErroValidacao.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE.getKey());
                }
            }

            if (request.getControleBalao() == EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA) {
                grupoIntermediarias.forEach(intermediaria -> {

                    validarIntevalo(limiteMaximoIntervaloResidual, intermediaria);

                    if (request.getRestricoesPercentuaisBalao() != null) {

                        Optional<RestricaoPercentualBalao> restricaoOptional = request.getRestricoesPercentuaisBalao().stream().filter(fil -> fil.getPrazoInicial() <= intermediaria.getNumeroParcela() && fil.getPrazoFinal() >= intermediaria.getNumeroParcela()).findFirst();

                        if (restricaoOptional.isPresent()) {
                            RestricaoPercentualBalao restricao = restricaoOptional.get();

                            final BigDecimal valorMinimoIntermedia = this.request.getValorBem().multiply(restricao.getMinimo().divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP));
                            final BigDecimal valorMaximoIntermedia = this.request.getValorBem().multiply(restricao.getMaximo().divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP));

                            validarLimiteIntermediaria(intermediaria.getValor(), valorMaximoIntermedia, valorMinimoIntermedia, restricao.getMinimo(), restricao.getMaximo());

                            long quantidaNoMesmoRange = grupoIntermediarias.stream().filter(grupo -> restricao.getPrazoInicial() <= grupo.getNumeroParcela() && restricao.getPrazoFinal() >= grupo.getNumeroParcela()).count();

                            if (quantidaNoMesmoRange > restricao.getQuantidadeIntermediariasRange()) {
                                throw new BusinessValidationException(null, restricao.getQuantidadeIntermediariasRange().toString(), String.format("Quantidade de intermediárias para parcelas de %S a %S, é %S.", restricao.getPrazoInicial(), restricao.getPrazoFinal(), restricao.getQuantidadeIntermediariasRange().toString()), EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());

                            }
                        }
                    }
                });
            }
        }
    }

    private void validarIntevalo(long limiteMaximoIntervaloResidual, ParcelaIntermediaria intermediaria) {
        if (intermediaria.getNumeroParcela() > limiteMaximoIntervaloResidual)
            throw new BusinessValidationException(null, "", "Parcela intermediária " + intermediaria.getVencimento() + "(" + intermediaria.getNumeroParcela() + ") " + "ultrapassou o intervalo entre a parcela residual.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
    }

    private void validaQuatidadeIntermediarias() {
        if (request.getIntermediarias() != null && request.getIntermediarias().size() > 0) {

            if (request.getRestricoesPercentuaisBalao() != null && request.getIntermediarias().size() > 1) {
                final long quantidadeIntermediarias = request.getIntermediarias().size();
                final int limiteParcelaParaInserirIntermediaria = prazo - (request.getIntervaloResidual() == null ? 0 : request.getIntervaloResidual() + 1);

                if (quantidadeIntermediarias > request.getQuantidadeIntermediarias()) {
                    throw new BusinessValidationException(null, "", "O numero de intermediárias é maior que o permitido no plano.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
                }
                if (totalIntermediariasComRestricoesUsadas(limiteParcelaParaInserirIntermediaria)
                        > totalIntermediariasComRestricoesPermitidasPorLimite(limiteParcelaParaInserirIntermediaria)) {
                    throw new BusinessValidationException(null, "", "Intermediária fora do range permitido.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
                }
                validarQuantidadeIntermediariasSemRestricoes(limiteParcelaParaInserirIntermediaria);
            } else if (request.getIntermediarias().size() > request.getQuantidadeIntermediarias()) {
                throw new BusinessValidationException(null, "", "A configuração do plano permite apenas " + request.getQuantidadeIntermediarias() + " intermediárias.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
            }
        }
    }

    private void validarQuantidadeIntermediariasSemRestricoes(int parcelaLimite) {

        if (request.getIntermediarias() != null && request.getIntermediarias().size() > 1) {
            Collections.sort(request.getIntermediarias(), Comparator.comparingLong(ParcelaIntermediaria::getNumeroParcela));
            request.getIntermediarias().forEach(
                    item -> item.setNumeroParcela(primeiroPagamento.until(item.getVencimento(), ChronoUnit.MONTHS) + 1)
            );
        }
        int totalDeParcelasSemRestricoes = 0;
        int totalDeParcelasSemRestricoesUsadas = 0;

        int totalDeParcelasSemRestricoesPermitidas = (request.getQuantidadeIntermediarias() - totalIntermediariasComRestricoesPermitidas()) - 1;

        if (totalDeParcelasSemRestricoesPermitidas > 0) {
            int recontaSemRestricoes = 0;
            for (int i = 1; i < parcelaLimite - 1; i++) {
                long intermediaria = i;
                if (request.getRestricoesPercentuaisBalao()
                        .stream()
                        .filter(filtroRestricao ->
                                filtroRestricao.getPrazoInicial() >= intermediaria
                                        && filtroRestricao.getPrazoFinal() <= intermediaria).count() == 0) {
                    recontaSemRestricoes++;
                    if (recontaSemRestricoes == totalDeParcelasSemRestricoesPermitidas) {
                        break;
                    }
                }
            }

            totalDeParcelasSemRestricoesPermitidas = recontaSemRestricoes;

            for (int i = 0; i < request.getIntermediarias().size() - 1; i++) {
                long intermediaria = request.getIntermediarias().get(i).getNumeroParcela();
                if (request.getRestricoesPercentuaisBalao()
                        .stream()
                        .filter(filtroRestricao ->
                                filtroRestricao.getPrazoInicial() <= intermediaria
                                        && filtroRestricao.getPrazoFinal() >= intermediaria).count() == 0) {
                    totalDeParcelasSemRestricoesUsadas++;
                }
            }
        }

        if ((totalDeParcelasSemRestricoes + totalIntermediariasComRestricoesUsadas(parcelaLimite)) > (request.getQuantidadeIntermediarias() - 1)) {
            throw new BusinessValidationException(null, "", "Intermediária(s) fora do range permitido.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
        }
        if (totalDeParcelasSemRestricoesUsadas > totalDeParcelasSemRestricoesPermitidas) {
            throw new BusinessValidationException(null, "", "Intermediária(s) fora do range permitido.", EnumErroValidacao.ERRO_RESTRICOES_INTERMEDIARIAS.getKey());
        }
    }

    private long totalIntermediariasComRestricoesUsadas(long parcelaLimite) {

        long totalDeParcelasComRestricoes = 0;

        if (request.getIntermediarias() != null && request.getIntermediarias().size() > 1) {
            for (int i = 0; i < request.getIntermediarias().size() - 1; i++) {
                ParcelaIntermediaria intermediaria = request.getIntermediarias().get(i);
                if (request.getRestricoesPercentuaisBalao()
                        .stream()
                        .filter(filtroRestricao ->
                                filtroRestricao.getPrazoInicial() >= parcelaLimite
                                        && filtroRestricao.getPrazoFinal() <= intermediaria.getNumeroParcela()).count() > 0) {
                    totalDeParcelasComRestricoes++;
                }
            }
        }
        return totalDeParcelasComRestricoes;
    }

    private int totalIntermediariasComRestricoesPermitidasPorLimite(int parcelaLimite) {

        int totalDeParcelasComRestricoes = 0;

        if (request.getRestricoesPercentuaisBalao() != null) {
            totalDeParcelasComRestricoes = request.getRestricoesPercentuaisBalao().stream().filter(filtroRestricao -> filtroRestricao.getPrazoInicial() <= parcelaLimite).map(restricao -> {
                if ((parcelaLimite - (restricao.getPrazoInicial() - 1)) >= restricao.getQuantidadeIntermediariasRange()) {
                    return restricao.getQuantidadeIntermediariasRange();
                } else {
                    return 1;
                }
            }).reduce((x, y) -> x + y).orElse(0);
        }
        return totalDeParcelasComRestricoes;
    }

    private int totalIntermediariasComRestricoesPermitidas() {

        int totalDeParcelasComRestricoes = 0;

        if (request.getRestricoesPercentuaisBalao() != null && request.getRestricoesPercentuaisBalao().size() > 0) {
            totalDeParcelasComRestricoes = request.getRestricoesPercentuaisBalao().stream()
                    .map(RestricaoPercentualBalao::getQuantidadeIntermediariasRange).reduce((x, y) -> x + y).orElse(0);
        }
        return totalDeParcelasComRestricoes;
    }

    private LocalDate getDataVencimento(LocalDate vencimento) {
        if (Objects.isNull(primeiroPagamento))
            return vencimento.withDayOfMonth(1);

        boolean ehPrimeiroPagamento = primeiroPagamento.isEqual(vencimento);
        return ehPrimeiroPagamento ? vencimento : vencimento.withDayOfMonth(1);
    }

}
