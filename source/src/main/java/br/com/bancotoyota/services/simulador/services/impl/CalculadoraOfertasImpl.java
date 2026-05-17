package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.*;

@Service
@Slf4j
public class CalculadoraOfertasImpl implements CalculadoraOfertas {

    @Autowired
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @Autowired
    @Lazy
    private CalculadoraParcelasResidual calculadoraParcelasResidual;

    @Override
    public void adicionarIntermediariasNoCalculoDasOfertas(SimulacaoParcResidualRequest request, ResultadoParcelaResidual resultadoSimulacao
            , boolean validarSubsidio, boolean somenteFluxo, boolean corrigirMaximo, LocalDate dataCalculo, List<Prazo> prazos
            , TipoDeSeguroPrestamista tipoFixo, boolean validarResidual, SimulacaoParcResidual simulacao) {
        // Retorna a origem da solicitação para descobrir as parcelas intermediárias
        request.setOrigemSolicitacao(GERADOR_OFERTA.getValue());
        List<Prazo> finalPrazos = prazos;

        for (int indiceCalculo = 0; indiceCalculo < resultadoSimulacao.getList().size(); indiceCalculo++) {
            int prazoDesejado = resultadoSimulacao.getList().get(indiceCalculo).getPrazo();
            request.setIntermediarias(null);
            Optional<Prazo> prazoComConfiguracao = finalPrazos.stream().filter(p -> p.getNumeroDeParcelas().equals(prazoDesejado)).findFirst();

            if (Objects.nonNull(prazoComConfiguracao.get()) && Objects.nonNull(prazoComConfiguracao.get().getNumeroDeParcelas()) && prazoComConfiguracao.get().getNumeroDeParcelas() > 0) {
                Prazo prazoSelecionado = prazoComConfiguracao.get();
                List<Prazo> prazoTemp = new ArrayList<>();
                prazoTemp.add(prazoSelecionado);

                request.setPrazoDesejado(prazoDesejado);
                request.setParcelas(new Integer[]{prazoDesejado});
                request.setPermiteBalao(true);
                request.setDataPrimeiroVencimento(dataCalculo);

                if (Objects.isNull(request.getIntermediarias())) {
                    List<ParcelaIntermediaria> novasIntermediarias = new ArrayList<>();
                    List<ParcelaBalao> parcelaBalao = new ArrayList<>();

                    BigDecimal valorMinimoResidual = simulacao.obterValorResidualMinimo(request.getValorBem(), prazoSelecionado.getPercentualMinBalao(), null);
                    BigDecimal valorMaximoResidual = simulacao.obterValorResidualMaximo(request.getValorBem(), prazoSelecionado.getPercentualMaxBalao());

                    simulacao.montarResidualDoRequest(request, prazoSelecionado, parcelaBalao, valorMaximoResidual, valorMinimoResidual, dataCalculo, false);

                    if (!parcelaBalao.isEmpty()) {
                        ParcelaIntermediaria intermediaria = new ParcelaIntermediaria();
                        intermediaria.setValor(parcelaBalao.get(0).getValor());
                        intermediaria.setVencimento(this.obterDataVencimentoComDiaFixo(parcelaBalao.get(0).getData()));
                        novasIntermediarias.add(intermediaria);
                        request.setIntermediarias(novasIntermediarias);
                    }
                }

                List<ParcelaIntermediaria> intermediarias = obterIntermediarias(request);
                if (!intermediarias.isEmpty()) {
                    intermediarias.addAll(request.getIntermediarias());
                    request.setIntermediarias(intermediarias);
                }

                try {
                    // Recria a simulação com as informações das parcelas intermediárias
                    SimulacaoParcResidual recalculoSimulacao = calculadoraParcelasResidual.criarSimulacao(request, validarSubsidio, prazoTemp, corrigirMaximo, dataCalculo, validarResidual);
                    ResultadoParcelaResidual resultadoNovaSimulacao = calculadoraParcelasResidual.executarSimulacao(somenteFluxo, prazoTemp, tipoFixo, recalculoSimulacao);

                    if (Objects.nonNull(resultadoNovaSimulacao.getList()) && !resultadoNovaSimulacao.getList().isEmpty()) {
                        resultadoSimulacao.getList().set(indiceCalculo, resultadoNovaSimulacao.getList().get(0));
                    }
                } catch (Exception ex) {
                    log.info("Erro o calcular ofertas com parcelas intermediárias" + ex);
                }
            }
        }
    }

    private List<ParcelaIntermediaria> obterIntermediarias(SimulacaoParcResidualRequest request) {
        if (Objects.nonNull(request.getOrigemSolicitacao()) && request.getOrigemSolicitacao().equals("GERADOR_OFERTA")) {
            PlanoMotorTaxa plano = motorTaxaPlanoService.getPlanoPorId(request.getPlanoId());

            if (Objects.nonNull(plano.getParcelasIntermediarias()) &&
                    Objects.nonNull(plano.getParcelasIntermediarias().getPermiteBalao()) &&
                    Objects.nonNull(plano.getParcelasIntermediarias().getQuantidadeMaximaBalao()) &&
                    Objects.nonNull(plano.getParcelasIntermediarias().getIntervaloResidual()) &&
                    plano.getParcelasIntermediarias().getIntervaloResidual() >= 0 &&
                    Objects.nonNull(request.getPrazoDesejado()) && request.getPrazoDesejado() > 0 &&
                    Boolean.TRUE.equals(plano.getParcelasIntermediarias().getPermiteBalao())) {

                Integer prazoDesejado = request.getPrazoDesejado();
                BigDecimal maxIntermediarias = BigDecimal.valueOf(plano.getParcelasIntermediarias().getQuantidadeMaximaBalao() - 1);

                BigDecimal percentualMinimoFinanciamento = (request.getPercentualMinimoFinanciamento() == null ?
                        BigDecimal.ZERO : request.getPercentualMinimoFinanciamento());

                BigDecimal percentualEntrada = request.getPercentualEntrada();

                BigDecimal percentualResidual = request.getValorParcelaResidual()
                        .divide(request.getValorBem(), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                BigDecimal percentualDivisaoIntermediarias = BigDecimal.valueOf(100).subtract(
                        (percentualMinimoFinanciamento.add(percentualEntrada).add(percentualResidual)));

                return obterIntermediariasPossiveis(percentualDivisaoIntermediarias, maxIntermediarias, prazoDesejado, request.getValorBem(),
                        request.getPeriodicidade().longValue(), plano, request.getDataPrimeiroVencimento());
            }
        }
        return new ArrayList<>();
    }

    private List<ParcelaIntermediaria> obterIntermediariasPossiveis(BigDecimal percentualDivisaoIntermediarias, BigDecimal maxIntermediarias,
                                                                    Integer prazoDesejado, BigDecimal valorBem, Long periodicidade, PlanoMotorTaxa plano, LocalDate primeiroVencimento) {
        if (maxIntermediarias.compareTo(BigDecimal.ZERO) == 0) {
            return new ArrayList<>();
        }

        BigDecimal percentualMaxCadaIntermediaria = percentualDivisaoIntermediarias.divide(maxIntermediarias, 8, RoundingMode.HALF_UP);

        if (Objects.isNull(plano.getParcelasIntermediarias().getRestricoesPercentuaisBalao()) || plano.getParcelasIntermediarias().getRestricoesPercentuaisBalao().isEmpty()) {
            return new ArrayList<>();
        }

        List<RestricaoPercentualBalao> parcelasIntermediariasPossiveis = plano.getParcelasIntermediarias()
                .getRestricoesPercentuaisBalao().stream().filter(
                        balao -> balao.getPrazoInicial() <= prazoDesejado)
                .collect(Collectors.toList());

        List<RestricaoPercentualBalao> balaoParcelasIntermediarias = parcelasIntermediariasPossiveis.stream().filter(
                        balao -> balao.getMaximo().compareTo(percentualMaxCadaIntermediaria) >= 0 &&
                                balao.getMinimo().compareTo(percentualMaxCadaIntermediaria) <= 0)
                .collect(Collectors.toList());

        List<ParcelaIntermediaria> intermediarias = new ArrayList<>();
        if (balaoParcelasIntermediarias.isEmpty()) {
            return this.obterIntermediariasPossiveis(percentualDivisaoIntermediarias, maxIntermediarias.subtract(BigDecimal.ONE), prazoDesejado, valorBem, periodicidade, plano, primeiroVencimento);
        } else {
            int intervaloIntermediarias = plano.getParcelasIntermediarias().getIntervaloResidual() > 0 ? plano.getParcelasIntermediarias().getIntervaloResidual() : 1;
            int ajusteNumeroParcelaComoIndice = 1;
            AtomicInteger numeroParcela = new AtomicInteger(prazoDesejado - intervaloIntermediarias);
            Collections.reverse(balaoParcelasIntermediarias);

            int intermediariasRestantes = Integer.parseInt(maxIntermediarias.toString());
            while (intermediariasRestantes > 0) {
                if (numeroParcela.get() < 1) {
                    intermediariasRestantes = 0;
                }

                Optional<RestricaoPercentualBalao> percentualBalao = parcelasIntermediariasPossiveis.stream().filter(balao ->
                        balao.getPrazoInicial() <= numeroParcela.get() && balao.getPrazoFinal() >= numeroParcela.get()
                ).findFirst();

                if (percentualBalao.isPresent() &&
                        Objects.nonNull(percentualBalao.get().getQuantidadeIntermediariasRange()) &&
                        percentualBalao.get().getQuantidadeIntermediariasRange() > 0 &&
                        (intermediarias.size() + 1) <= percentualBalao.get().getQuantidadeIntermediariasRange()
                ) {
                    if (
                            intermediarias.stream().filter(intermediaria ->
                                    intermediaria.getNumeroParcela() >= percentualBalao.get().getPrazoInicial()
                                            && intermediaria.getNumeroParcela() <= percentualBalao.get().getPrazoFinal()
                            ).count() < percentualBalao.get().getQuantidadeIntermediariasRange()) {
                        ParcelaIntermediaria parcelaIntermediaria = new ParcelaIntermediaria();
                        parcelaIntermediaria.setValor(percentualMaxCadaIntermediaria.multiply(valorBem).divide(BigDecimal.valueOf(100)));
                        LocalDate dataVencimento = primeiroVencimento
                                .plusMonths((long) (numeroParcela.get() - ajusteNumeroParcelaComoIndice) * periodicidade);
                        parcelaIntermediaria.setVencimento(this.obterDataVencimentoComDiaFixo(dataVencimento));

                        intermediarias.add(parcelaIntermediaria);
                        intermediariasRestantes--;
                    } else {
                        numeroParcela.set(percentualBalao.get().getPrazoInicial());
                    }
                }

                numeroParcela.addAndGet(-intervaloIntermediarias);
            }
        }
        return intermediarias;
    }

    private LocalDate obterDataVencimentoComDiaFixo(LocalDate dataVencimento) {
        // Fixado o dia primeiro do mês porque durante a validação das intermediárias
        // é utilizado o dia 1, isto para não calcular errado a diferença em meses de duas datas
        return LocalDate.of(dataVencimento.getYear(), dataVencimento.getMonth(), 1);
    }
}