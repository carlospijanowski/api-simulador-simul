package br.com.bancotoyota.services.simulador.utils;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoRequest;
import br.com.bancotoyota.services.simulador.beans.response.Fluxo;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.services.FluxoService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class PrazoMaisProximoValorDesejadoUtils {

    public static Boolean selecionarPrazoMaisProximo(SimulacaoRequest finalRequest, BigDecimal limiteDiferencaEmReais,
                                                     List<? extends PrazoResidualResponse> prazos, LocalDate dataBase) {
        BigDecimal desejada = finalRequest.getValorParcelaDesejada();
        PrazoResidualResponse prazoSelecionado = null;
        if (desejada != null) {
            PrazoResidualResponse prazoNoLimite = null;
            PrazoResidualResponse prazoAbaixo = null;
            PrazoResidualResponse prazoMaisProximo = null;

            // tirando cópia da lista para não alterar a lista original
            prazos = new ArrayList<>(prazos);
            // vamos processar os prazos na ordem inversa para dar prioridade para os maiores prazos
            Collections.reverse(prazos);

            Iterator<? extends PrazoResidualResponse> i = prazos.iterator();
            if (i.hasNext()) {
                prazoMaisProximo = i.next();
                BigDecimal diferencaAbaixo = desejada.subtract(prazoMaisProximo.getValorParcela());
                BigDecimal diferenca = diferencaAbaixo.abs();
                if (diferencaAbaixo.signum() >= 0) {
                    prazoAbaixo = prazoMaisProximo;
                } else {
                    diferencaAbaixo = new BigDecimal(Long.MAX_VALUE);
                }
                BigDecimal limiteInicial = desejada.subtract(limiteDiferencaEmReais);
                BigDecimal limiteFinal = desejada.add(limiteDiferencaEmReais);
                log.debug("Limite Inicial: " + limiteInicial);
                log.debug("Limite calculado: " + limiteFinal);
                log.debug("diferença " + prazoMaisProximo.getParcelas() + ": " + diferenca + " - " + prazoMaisProximo.getValorParcela());
                if (atingiuValor(limiteInicial, limiteFinal, prazoMaisProximo.getValorParcela())) {
                    prazoNoLimite = prazoMaisProximo;
                }

                while (i.hasNext() && prazoNoLimite == null) {
                    PrazoResidualResponse p = i.next();
                    BigDecimal diferencaNovaAbaixo = desejada.subtract(p.getValorParcela());
                    BigDecimal diferencaNova = diferencaNovaAbaixo.abs();
                    log.debug("diferença " + p.getParcelas() + ": " + diferencaNova + " - " + p.getValorParcela());
                    // vamos considerar a diferença nova somente se ela for menor do que a diferença atual para
                    // dar prioridade para o prazo maior que foi visto antes
                    if (diferencaNova.compareTo(diferenca) < 0) {
                        diferenca = diferencaNova;
                        prazoMaisProximo = p;
                    }

                    if (diferencaNovaAbaixo.signum() >= 0 && diferencaNovaAbaixo.compareTo(diferencaAbaixo) < 0) {
                        prazoAbaixo = p;
                        diferencaAbaixo = diferencaNovaAbaixo;
                    }

                    if (atingiuValor(limiteInicial, limiteFinal, p.getValorParcela())) {
                        prazoNoLimite = p;
                    }
                }

            }

            if (prazoNoLimite != null) {
                prazoSelecionado = prazoNoLimite;
            } else if (prazoAbaixo != null) {
                // se nenhum prazo esteva dentro do limite vamos usar o maior que está abaixo da parcela desejada
                prazoSelecionado = prazoAbaixo;
            }

            PrazoResidualResponse prazoParaFluxo = prazoMaisProximo;
            if (prazoSelecionado != null) {
                prazoParaFluxo = prazoSelecionado;
            }

            if (prazoParaFluxo != null) {
                List<Fluxo> fluxo = FluxoService.getInstance().getFluxo(prazoParaFluxo.getResultadoCalculado(), finalRequest.getValorEntrada(),
                        dataBase);
                prazoParaFluxo.setFluxo(fluxo);
            }
        }
        return prazoSelecionado != null;
    }

    private static boolean atingiuValor(BigDecimal inicio, BigDecimal fim , BigDecimal valor) {
        return valor.compareTo(inicio) >= 0 && valor.compareTo(fim) <= 0;
    }
}
