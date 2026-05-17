package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.ResultadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.controller.SimulacaoParcResidual;
import br.com.bancotoyota.services.simulador.controller.SimuladorExceptionController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ResidualService implements InitializingBean {

    public static final BigDecimal PRECISAO = new BigDecimal("0.10");
    public static final String VALOR_DESEJADO = " valor desejado: ";

    /**
     * Quando rodamos unit tests vazemos algumas validações que não são necessárias quando está rodando.
     * Isso é bem útil pois evita que cada um dos testes tenha que fazer essas validações.
     */
    private boolean unitTest = true;

    @Autowired
    private SimuladorParcResidualController simuladorService;
    
    @Autowired
    @Setter
    private ObjectMapper mapper;      

    @Override
    public void afterPropertiesSet() {
        unitTest = isJUnitTest();
    }

    /**
     * Para usar a injeção via SpringBoot
     */
    public ResidualService() {}

    /**
     * Usado em testes unitários
     */
    public ResidualService(SimuladorParcResidualController simuladorService) {
        this.simuladorService = simuladorService;
        afterPropertiesSet();
    }

    public ResultadoParcelaResidual parcelaDesejada(SimulacaoParcResidualRequest request, boolean corrigirMaximo) {
        if (request.getModalidade() != Modalidade.CICLO_TOYOTA && request.getModalidade() != null) {
            throw new BusinessValidationException("residual só pode ser usado com plano ciclo-toyota");
        }

        if (request.getValorParcelaDesejada() == null) {
            throw new InvalidFieldException("valor-parcela-desejada", null, "campo não informado");
        }

        LocalDate dataCalculo = simuladorService.getDataCalculo(request);

        return encontrarValorDosResiduais(request, dataCalculo, corrigirMaximo);
    }

    ResultadoParcelaResidual encontrarValorDosResiduais(SimulacaoParcResidualRequest request, LocalDate dataCalculo, boolean corrigirMaximo) {
        List<Prazo> prazos = simuladorService.carregarPrazos(request);
        BigDecimal desejada = request.getValorParcelaDesejada();

        List<ResultadoCalculadoParcelaResidual> list = new ArrayList<>(prazos.size());

        // colocamos o valor zero só pra passar na validação, em seguida o valor do residual será determinado para cada prazo
        request.setValorParcelaResidual(BigDecimal.ZERO);
        SimulacaoParcResidual simulacao = simuladorService.criarSimulacao(request, true, prazos, corrigirMaximo, dataCalculo, false);

        validaPercentualInicial(getMaxPercentualParcelaResidual(request), prazos, request.getValorEntrada());

        for (int i = 0; i < prazos.size(); i++) {
            List<Prazo> prazoAtual = prazos.subList(i, i + 1);
            ResultadoCalculadoParcelaResidual r = encontrarResidualParaPrazo(request, simulacao, desejada, prazoAtual);
            list.add(r);
            if (unitTest) {
                validarSimulacao(request, r, prazoAtual.get(0).getNumeroDeParcelas());
            }
        }

        if (unitTest) {
            validarIntercalamentoDoPrestamista(list);
        }

        return new ResultadoParcelaResidual(list, simulacao.getLimitesSubsidioValor().getDadosDoSubsidio());
    }

    @SneakyThrows
    private void validarSimulacao(SimulacaoParcResidualRequest request, ResultadoCalculadoParcelaResidual resultado, Integer prazo) {
        request.setValorParcelaResidual(resultado.getValorParcelaResidual());
        request.setParcelas(new Integer[] { prazo });
        PrazoResidualResponse resultado2 = simuladorService.getParcelaResidual(null, request).getBody().getPrazos().get(0);
        if (resultado2.getValorParcela().compareTo(resultado.getValorParcela().setScale(2, RoundingMode.HALF_UP)) != 0
                || resultado2.getTipoDeSeguroPrestamista() != resultado.getDadosSeguroPrestamista().getTipo()) {
            throw new IllegalStateException("simulação com os valores calculados não gerou o mesmo valor de parcela ou " +
                    "tipo de seguro prestamista. Prazo: " + prazo + " Residual: " + resultado.getValorParcelaResidual());
        }
    }

    private void validarIntercalamentoDoPrestamista(List<ResultadoCalculadoParcelaResidual> list) {
        TipoDeSeguroPrestamista tipo = null;
        for (ResultadoCalculadoParcelaResidual rc : list) {
            TipoDeSeguroPrestamista tipoNovo = rc.getDadosSeguroPrestamista().getTipo();
            if (tipo == null) {
                tipo = tipoNovo;
            } else if (tipo != tipoNovo) {
                if (tipo == TipoDeSeguroPrestamista.SPF && tipoNovo != TipoDeSeguroPrestamista.SPF) {
                    throw new IllegalStateException("uma vez que um prazo entrou para SPF todos os prazos maiores tem que usar SPF também, pois o valor da parcela só vai diminuir");
                }
                tipo = tipoNovo;
            }
        }
    }

    private ResultadoCalculadoParcelaResidual encontrarResidualParaPrazo(SimulacaoParcResidualRequest request, SimulacaoParcResidual simulacao, BigDecimal desejada,
                                            List<Prazo> prazoAtual) {
        BigDecimal[] limites = calcularLimites(prazoAtual.get(0), request);

        // valor máximo para a intermediária
        BigDecimal valorBIntermediaria = limites[1];
        ResultadoCalculadoParcelaResidual rB = fazerSimulacao(valorBIntermediaria, request, simulacao, prazoAtual, null);
        TipoDeSeguroPrestamista tipoB = rB.getDadosSeguroPrestamista().getTipo();
        // menor parcela possível
        BigDecimal valorBParcela = rB.getValorParcela();

        boolean valorMinimoMaiorQueDesejada = valorBParcela.compareTo(desejada) >= 0;
        log.debug("valor máximo intermediaria: " + valorBIntermediaria + " menor valor da parcela: "
                + valorBParcela + VALOR_DESEJADO + desejada);

        if (valorMinimoMaiorQueDesejada) {
            // Se o tipo do prestamista usado foi o SPF talvez ainda seja possível atingir o valor da parcela mudando
            // o seguro para SVP ou para nenhum, mas como a prioridade é para usar o SPF não iremos explorar essa
            // possibilidade. Para trocar para SVP de alguma forma precisamos de uma forma de forçar isso pois se formos
            // executar essa simulação novamente usando o mesmo residual a prioridade será dada para o SPF e o valor da
            // parcela mudará.
            return rB;
        } else {
            return verificarLimiteSuperior(request, simulacao, desejada, prazoAtual, limites[0], valorBIntermediaria, tipoB, valorBParcela);
        }
    }

    private ResultadoCalculadoParcelaResidual verificarLimiteSuperior(SimulacaoParcResidualRequest request,
                                                                      SimulacaoParcResidual simulacao, BigDecimal desejada,
                                                                      List<Prazo> prazoAtual, BigDecimal limite,
                                                                      BigDecimal valorBIntermediaria,
                                                                      TipoDeSeguroPrestamista tipoB, BigDecimal valorBParcela) {
        BigDecimal valorAIntermediaria = limite;
        ResultadoCalculadoParcelaResidual rA = fazerSimulacao(valorAIntermediaria, request, simulacao, prazoAtual, null);
        TipoDeSeguroPrestamista tipoA = rA.getDadosSeguroPrestamista().getTipo();
        BigDecimal valorAParcela = rA.getValorParcela();
        boolean valorMaximoMenorQueDesejada = valorAParcela.compareTo(desejada) <= 0;
        log.debug("valor mínimo intermediaria: " + valorAIntermediaria + " maior valor da parcela: "
                + valorAParcela + VALOR_DESEJADO + desejada);

        if (valorMaximoMenorQueDesejada) {
            if (tipoB == TipoDeSeguroPrestamista.SPF && tipoA != TipoDeSeguroPrestamista.SPF) {
                // Como tipoB é SPF obviamente que esse cliente pode usar o SPF.
                // Se a ponta inferior deu SPF e a superior deu outra coisa talvez seja possível atingir o valor
                // desejado mudando para SPF. Precisamos verificar essa possibilidade pois o SPF tem prioridade
                // e também para garantir que não teremos prazos vizinhos com SPF e o do meio com outro tipo.
                ResultadoCalculadoParcelaResidual rA2 = fazerSimulacao(valorAIntermediaria, request, simulacao, prazoAtual, TipoDeSeguroPrestamista.SPF);
                BigDecimal valorAParcela2 = rA2.getValorParcela();
                valorMaximoMenorQueDesejada = valorAParcela2.compareTo(desejada) <= 0;
                log.debug("valor mínimo intermediaria: " + valorAIntermediaria + " maior valor da parcela: "
                        + valorAParcela2 + VALOR_DESEJADO + desejada);
                Seguro spf = simulacao.getSPF();
                if (valorMaximoMenorQueDesejada) {
                    if (valorAParcela2.compareTo(spf.getMaximoDaParcela()) > 0 && spf.getMaximoDaParcela().compareTo(valorAParcela) >= 0) {
                        BigDecimal residualMinimo = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria,
                                valorAParcela2, valorBParcela, spf.getMaximoDaParcela());
                        return fazerSimulacao(residualMinimo, request, simulacao, prazoAtual, null);
                    } else {
                        // nesse caso teremos que usar SVP/NENHUM mesmo, porque usando SPF o valor também não chegou devido
                        // a limitação de valor da parcela
                        return rA;
                    }
                } else {
                    return calcularERetornar(request, simulacao, desejada, prazoAtual, valorBIntermediaria, tipoB, valorBParcela, valorAIntermediaria, tipoB, valorAParcela2);
                }
            }
            return rA;
        } else {
            return calcularERetornar(request, simulacao, desejada, prazoAtual, valorBIntermediaria, tipoB, valorBParcela, valorAIntermediaria, tipoA, valorAParcela);
        }
    }

    private ResultadoCalculadoParcelaResidual calcularERetornar(SimulacaoParcResidualRequest request, SimulacaoParcResidual simulacao, BigDecimal desejada, List<Prazo> prazoAtual, BigDecimal valorBIntermediaria, TipoDeSeguroPrestamista tipoB, BigDecimal valorBParcela, BigDecimal valorAIntermediaria, TipoDeSeguroPrestamista tipoA, BigDecimal valorAParcela) {
        BigDecimal residualDesejado;
        // Os tipos só serão diferentes caso o tipo B (o da menor parcela) seja SPF pois é o único que tem
        // limitação no valor da parcela.
        if ((tipoB == TipoDeSeguroPrestamista.SPF || tipoB == TipoDeSeguroPrestamista.NENHUM) && tipoA != TipoDeSeguroPrestamista.SPF) {
            return calcularValorDaIntermediariaComPrestamistaDiferente(request, simulacao,
                    desejada, prazoAtual, valorAIntermediaria, tipoA, valorAParcela, valorBIntermediaria,
                    tipoB, valorBParcela);
        } else {
            // aqui tipoA é igual a tipoB
            residualDesejado = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria,
                    valorAParcela, valorBParcela, desejada);
            return validarERetornar(request, simulacao, desejada, prazoAtual, valorAIntermediaria, tipoA,
                    valorAParcela, valorBIntermediaria, tipoB, valorBParcela, residualDesejado);
        }
    }

    private ResultadoCalculadoParcelaResidual validarERetornar(SimulacaoParcResidualRequest request,
                                                               SimulacaoParcResidual simulacao, BigDecimal desejada,
                                                               List<Prazo> prazoAtual, BigDecimal valorAIntermediaria,
                                                               TipoDeSeguroPrestamista tipoA, BigDecimal valorAParcela,
                                                               BigDecimal valorBIntermediaria,
                                                               TipoDeSeguroPrestamista tipoB, BigDecimal valorBParcela,
                                                               BigDecimal residualDesejado) {

        if (tipoA != tipoB) {
            throw new IllegalStateException("este método deve ser chamado somente com os tipo de subsídio iguais");
        }

        if (tipoA == TipoDeSeguroPrestamista.SPF && desejada.compareTo(simulacao.getSPF().getMaximoDaParcela()) > 0) {
            BigDecimal residualMinimo = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria,
                    valorAParcela, valorBParcela, simulacao.getSPF().getMaximoDaParcela());
            if (residualDesejado.compareTo(residualMinimo) < 0) {
                return fazerSimulacao(residualMinimo, request, simulacao, prazoAtual, null);
            }
        }

        ResultadoCalculadoParcelaResidual r = fazerSimulacao(residualDesejado, request, simulacao, prazoAtual, null);

        // se o valor não bater exatamente vamos fazer só mais uma simulação se os tipos de prestamista forem os mesmos
        if (r.getValorParcela().setScale(2, RoundingMode.HALF_UP).subtract(desejada).compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal residualProximo = residualDesejado;

            if (r.getDadosSeguroPrestamista().getTipo() == tipoB) {
                residualDesejado = calcularValorDaIntermediaria(residualProximo, valorBIntermediaria,
                        r.getValorParcela(), valorBParcela, desejada);
                r = fazerSimulacao(residualDesejado, request, simulacao, prazoAtual, null);
                if (r.getValorParcela().setScale(2, RoundingMode.HALF_UP).subtract(desejada).compareTo(BigDecimal.ZERO) == 0) {
                    return r;
                }
            }

            if (r.getDadosSeguroPrestamista().getTipo() == tipoA) {
                residualDesejado = calcularValorDaIntermediaria(valorAIntermediaria, residualProximo,
                        valorAParcela, r.getValorParcela(), desejada);
                r = fazerSimulacao(residualDesejado, request, simulacao, prazoAtual, null);
                if (r.getValorParcela().setScale(2, RoundingMode.HALF_UP).subtract(desejada).compareTo(BigDecimal.ZERO) == 0) {
                    return r;
                }
            }

            if (r.getValorParcela().subtract(desejada).abs().compareTo(PRECISAO) > 0) {
                throw new IllegalStateException("valor desejado: " + desejada +
                        " - valor obtido: " + r.getValorParcela() + " - " + prazoAtual.get(0).getNumeroDeParcelas());
            }
        }
        return r;
    }

    private ResultadoCalculadoParcelaResidual calcularValorDaIntermediariaComPrestamistaDiferente(SimulacaoParcResidualRequest request,
                                                                           SimulacaoParcResidual simulacao, BigDecimal desejada,
                                                                           List<Prazo> prazoAtual, BigDecimal valorAIntermediaria,
                                                                           TipoDeSeguroPrestamista tipoA, BigDecimal valorAParcela,
                                                                           BigDecimal valorBIntermediaria, TipoDeSeguroPrestamista tipoB,
                                                                           BigDecimal valorBParcela) {
        // não é necessário verificar se o SPF é disponível ou se foi removido, pois caso contrário o tipo já não teria dado SPF
        Seguro spf = simulacao.getSPF();
        if (spf!=null && spf.getMaximoDaParcela().compareTo(desejada) >= 0) {
            return usarTipoB(request, simulacao, prazoAtual, valorAIntermediaria,
                    valorBIntermediaria, tipoB, valorBParcela);
        } else {
            return usarTipoA(request, simulacao, prazoAtual, valorAIntermediaria,
                    valorBIntermediaria, tipoA, valorAParcela);
        }
    }

    private BigDecimal[] calcularLimites(Prazo prazo, SimulacaoParcResidualRequest request) {
        BigDecimal min = prazo.getPercentualMinBalao().multiply(request.getValorBem()).divide(NumberUtils.CEM);
        BigDecimal max = prazo.getPercentualMaxBalao().multiply(request.getValorBem()).divide(NumberUtils.CEM);
     
        if (request.getPermiteBalao() != null
				&& !request.getPermiteBalao()) {
			min = new BigDecimal(0);
			max= new BigDecimal(0);
		} 
        else if (min.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0) {
            // AutBank não aceita residual zero
            min = SimuladorExceptionController.UM_CENTESIMO;
        }else if (request.getValorBem().compareTo(request.getValorEntrada().add(max)) < 0) {
        	//Quando o (valor da entrada + valor max residual permitido pelo plano) for maior que o valor do bem,
        	//o valor maximo da residual deve ser ajustado para (valor do bem - valor entrada).
        	max = request.getValorBem().subtract(request.getValorEntrada());
        }
        return new BigDecimal[]{
                min,
                max
        };
    }

    private ResultadoCalculadoParcelaResidual usarTipoB(SimulacaoParcResidualRequest request, SimulacaoParcResidual simulacao, List<Prazo> prazos,
                                                 BigDecimal valorAIntermediaria, BigDecimal valorBIntermediaria,
                                                 TipoDeSeguroPrestamista tipoB, BigDecimal valorBParcela) {
        // atualizando valor para fazer a proporção correta
        BigDecimal novoValorAParcela = fazerSimulacaoSimples(valorAIntermediaria, request, simulacao, prazos, tipoB);
        BigDecimal residualDesejado = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria,
                novoValorAParcela, valorBParcela, request.getValorParcelaDesejada());

        return validarERetornar(request, simulacao, request.getValorParcelaDesejada(), prazos, valorAIntermediaria, tipoB,
                novoValorAParcela, valorBIntermediaria, tipoB, valorBParcela, residualDesejado);
    }

    private ResultadoCalculadoParcelaResidual usarTipoA(SimulacaoParcResidualRequest request, SimulacaoParcResidual simulacao, List<Prazo> prazos,
                                                 BigDecimal valorAIntermediaria, BigDecimal valorBIntermediaria,
                                                 TipoDeSeguroPrestamista tipoA, BigDecimal valorAParcela) {
        // atualizando valor para fazer a proporção correta
        BigDecimal novoValorBParcela = fazerSimulacaoSimples(valorBIntermediaria, request, simulacao, prazos, tipoA);
        BigDecimal residualDesejado = calcularValorDaIntermediaria(valorAIntermediaria, valorBIntermediaria,
                valorAParcela, novoValorBParcela, request.getValorParcelaDesejada());
        return validarERetornar(request, simulacao, request.getValorParcelaDesejada(), prazos, valorAIntermediaria, tipoA,
                valorAParcela, valorBIntermediaria, tipoA, novoValorBParcela, residualDesejado);
    }

    private BigDecimal calcularValorDaIntermediaria(BigDecimal valorIntermediariaInicial, BigDecimal valorIntermediariaFinal,
                                                                    BigDecimal valorParcelaInicial, BigDecimal valorParcelaFinal,
                                                                    BigDecimal valorParcelaDesejada) {

        // vimos que a variação do valor da parcela é linear em relação ao valor da intermediária, por isso vamos
        // determinar o valor candidato fazendo uma simples regra de três ao invés de chutar no meio do intervalo
        BigDecimal diferencaParcela = valorParcelaInicial.subtract(valorParcelaFinal);
        BigDecimal diferencaIntermediaria = valorIntermediariaInicial.subtract(valorIntermediariaFinal);

        BigDecimal diferencaParcelaDesejada = valorParcelaInicial.subtract(valorParcelaDesejada);
        BigDecimal diferencaIntermediariaDesejada = diferencaParcelaDesejada.multiply(diferencaIntermediaria).divide(
                diferencaParcela,10, RoundingMode.HALF_UP);
        return valorIntermediariaInicial.subtract(diferencaIntermediariaDesejada).setScale(2, RoundingMode.HALF_UP);
    }

    private ResultadoCalculadoParcelaResidual fazerSimulacao(BigDecimal valorIntermediaria, SimulacaoParcResidualRequest request,
                                                             SimulacaoParcResidual simulacao, List<Prazo> prazos,
                                                             TipoDeSeguroPrestamista tipoFixo) {
        request.setIntermediarias(null);
        request.setValorParcelaResidual(valorIntermediaria);
        request.setRetornarFluxoPrazoUnico(false);
        request.setRetornarValorNaParcela(true);
        ResultadoParcelaResidual response = simuladorService.executarSimulacao(false, prazos, tipoFixo, simulacao);
        return response.getList().get(0);
    }

    private BigDecimal fazerSimulacaoSimples(BigDecimal valorIntermediaria, SimulacaoParcResidualRequest request,
                                                             SimulacaoParcResidual simulacao, List<Prazo> prazos,
                                                             TipoDeSeguroPrestamista tipoFixo) {
        request.setIntermediarias(null);
        request.setValorParcelaResidual(valorIntermediaria);
        simulacao.limparSimulacao(prazos, tipoFixo);
        return simulacao.fazerCalculoSimples().get(0);
    }

    private void validaPercentualInicial(BigDecimal percentualParcelaResidual, List<Prazo> list, BigDecimal entrada) {
        list.stream().map(Prazo::getPercentualMinBalao).min(BigDecimal::compareTo).ifPresent(min -> {
            if (min.compareTo(percentualParcelaResidual) > 0) {
                throw new BusinessValidationException("valor-entrada", entrada.toString() ,"Valor da entrada muito alto. Entrada mais valor mínimo para a " +
                        "parcela residual não pode ser maior que o valor do bem.");
            }
        });
    }

    private BigDecimal getMaxPercentualParcelaResidual(SimulacaoParcResidualRequest request) {
        BigDecimal percentualEntrada = request.getValorEntrada().multiply(NumberUtils.CEM)
                .divide(request.getValorBem(), 5, RoundingMode.HALF_UP);
        return NumberUtils.CEM.subtract(percentualEntrada);
    }

    private boolean isJUnitTest() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> list = Arrays.asList(stackTrace);
        for (StackTraceElement element : list) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }
   

}

	
