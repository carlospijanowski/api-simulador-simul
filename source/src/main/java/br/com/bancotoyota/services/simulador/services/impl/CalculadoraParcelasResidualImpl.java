package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.config.*;
import br.com.bancotoyota.services.simulador.controller.*;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.*;
import br.com.bancotoyota.services.simulador.utils.*;
import br.com.bancotoyota.services.simulador.validadores.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.*;

@Service
@Slf4j
public class CalculadoraParcelasResidualImpl implements CalculadoraParcelasResidual {

    public static final String VALOR_SUBSIDIO = "valor-subsidio";
    public static final String PLANO_ID = "plano-id";
    private BigDecimal subsidioMinimoPorValor;
    public static final String TAXA_SUBSIDIO = "taxa-subsidio";

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private LogService logService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private PlanoService planoService;

    @Autowired
    private CalculadoraOfertas calculadoraOfertas;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private MotorTaxaPlanoService motorTaxaPlanoService;
    @Autowired
    private SimulacaoServices simulacaoServices;

    @Autowired
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;
    @Autowired
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;
    @Autowired
    private FeatureToggleConfig featureToggleConfig;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private SimulacaoParcResidual simulacaoParcResidual;

    private Plano plano;

    @Autowired
    private CalculadoraServices calculadoraServices;
    @Autowired
    private DataCarencia dataCarencia;

    @Autowired
    SeguroPrestamistaPlusServiceImpl prestamistaService;

    @Autowired
    public void CalculadoraParcelasResidual() {
        this.simulacaoParcResidual = new SimulacaoParcResidual(featureToggleConfig, seguroFranquiaServices);
    }

    private Map<Integer, BigDecimal> getValorMaximoDoSubsidioImpl(SimulacaoParcResidualRequest request,
                                                                  LocalDate dataCalculo, List<Prazo> prazos) {
        BigDecimal taxaDeSubsidio = request.getTaxaSubsidio();
        BigDecimal valorDoSubsidio = request.getValorSubsidio();
        String retorno = request.getRetorno();

        // a taxa zero significa o maior subsídio possível
        request.setTaxaSubsidio(BigDecimal.ZERO);
        request.setValorSubsidio(null);
        request.setRetorno("S");

        try {
            return fazerSimulacao(request, false, false, false, dataCalculo, prazos, null, false).getList().stream()
                    .collect(Collectors.toMap(ResultadoCalculadoParcelaResidual::getPrazo,
                            ResultadoCalculadoParcelaResidual::getValorSubsidio));
        } finally {
            request.setTaxaSubsidio(taxaDeSubsidio);
            request.setValorSubsidio(valorDoSubsidio);
            request.setRetorno(retorno);
        }
    }

    @Override
    public ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                                   boolean somenteFluxo, LocalDate dataCalculo, boolean validarResidual) {
        return fazerSimulacao(request, validarSubsidio, somenteFluxo, false, dataCalculo, null, null, validarResidual);
    }


    private ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                                    boolean somenteFluxo, boolean corrigirMaximo, LocalDate dataCalculo, List<Prazo> prazos,
                                                    TipoDeSeguroPrestamista tipoFixo, boolean validarResidual) {

        if (logService != null) {
            logService.logData(request, "parâmetros para parcela residual: ");
        }

        boolean executarSimulacaoPrazoAPrazo = false;

        // evitando chamar getParcelas mais de uma vez por ser uma chamada remota
        if (prazos == null) {
            prazos = carregarPrazos(request);

            if (Objects.nonNull(request.getOrigemSolicitacao())
                    && request.getOrigemSolicitacao().equals(GERADOR_OFERTA.getValue())
                    && Objects.nonNull(prazos)
                    && !prazos.isEmpty()) {
                executarSimulacaoPrazoAPrazo = true;
                // Remove temporariamente a origem da solicitação para obter os cálculos sem a regra "Intermediária na ultima parcela é obrigatório"
                // O residual será aplicado automaticamente
                request.setOrigemSolicitacao(null);
            }
        }

        SimulacaoParcResidual simulacao = criarSimulacao(request, validarSubsidio, prazos, corrigirMaximo, dataCalculo, validarResidual);
        ResultadoParcelaResidual resultadoSimulacao = executarSimulacao(somenteFluxo, prazos, tipoFixo, simulacao);

        if (executarSimulacaoPrazoAPrazo) {
            // Retorna a origem da solicitação para descobrir as parcelas intermediárias
            request.setOrigemSolicitacao(GERADOR_OFERTA.getValue());
            calculadoraOfertas.adicionarIntermediariasNoCalculoDasOfertas(request, resultadoSimulacao, validarSubsidio, somenteFluxo, corrigirMaximo, dataCalculo, prazos, tipoFixo, validarResidual, simulacao);
        }

        return resultadoSimulacao;
    }

    @Override
    public ResultadoParcelaResidual executarSimulacao(boolean somenteFluxo, List<Prazo> prazos,
                                                      TipoDeSeguroPrestamista tipoFixo, SimulacaoParcResidual simulacao) {
        simulacao.limparSimulacao(prazos, tipoFixo);
        return new ResultadoParcelaResidual(simulacao.fazerCalculo(somenteFluxo),
                simulacao.getLimitesSubsidioValor().getDadosDoSubsidio());
    }

    @Override
    public SimulacaoParcResidual criarSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                                List<Prazo> prazos, boolean corrigirMaximo, LocalDate dataCalculo, boolean validarResidual) {
        carregarDadosDoPlano(request);
        corrigirParcelaResidual(request);

        dataCarencia.isDentroCarencia(request, dataCalculo); // Valida a data do primeiro vencimento

        corrigirDataCalculo(request, dataCalculo);

        validarModalidadeNaSimulacao(request, prazos);

        validarValoresEntradaIntermediarias(request);

        // Realiza algumas verificações antes de validar o valor do residual nos ranges de percentuais.
        if (validarResidual && (!simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request)))
            if (request.getControleBalao() != null &&
                    (request.getControleBalao().equals(EnumControleBalao.PERMITE_BALAO_ULTIMA_PARCELA) ||
                            request.getControleBalao().equals(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA)) &&
                    request.getPercentuaisBalao() != null &&
                    !request.getPercentuaisBalao().isEmpty() &&
                    request.getPrazoDesejado() != null &&
                    request.getPrazoDesejado() >= 1) {
                validarValorResidualNoRangeBalao(request);
            }


        // Cálculo do valor do seguro: taxa do seguro * valor financiado onde esse
        // último é:
        // (valor do bem - entrada + cesta + registro de contrato + items +
        // emplacamento)
        BigDecimal valorFinanciado = request.calcularValorBaseParaCalculoDoSeguro();

        TaxasIOF taxasIOF = simulacaoServices.getTaxasIOF(request.getTipoPessoaEnum(), request.getIsentaIOF());
        IOF iof = new IOF(taxasIOF);

        String cnpjOrigemNegocio = null;
        List<Integer> segurosPrestamistasExcluidos = null;
        boolean removerTodosSPF = false;
        boolean removerTodosSVP = false;

        if (ObjectUtils.isNotEmpty(request.getParametrosDeSeguroPrestamista())) {
            cnpjOrigemNegocio = request.getParametrosDeSeguroPrestamista().getCnpjOrigemNegocio();
            segurosPrestamistasExcluidos = request.getParametrosDeSeguroPrestamista().getSegurosPrestamistasExcluidos();
            removerTodosSPF = request.getParametrosDeSeguroPrestamista().isSpfRemovido();
            removerTodosSVP = request.getParametrosDeSeguroPrestamista().isSvpRemovido();
        }

        DadosCalculo dadosCalculo = new DadosCalculo(request.getDataCalculo(), iof,
                prestamistaService.getSegurosPrestamista(cnpjOrigemNegocio, dataCalculo, segurosPrestamistasExcluidos, removerTodosSPF, removerTodosSVP),
                valorFinanciado, null, null, null);

        if (this.plano != null) {
            dadosCalculo.setPlano(this.plano);
        }

        // o limitePorPrazo é usado somente na simulação por valor
        LimitesSubsidioValor limitesSubsidioValor;
        if (validarSubsidio) {
            limitesSubsidioValor = validarSubsidios(request, prazos, corrigirMaximo, dataCalculo);
        } else {
            limitesSubsidioValor = new LimitesSubsidioValor(null, null);
        }

        return criarSimulacao(request, prazos, limitesSubsidioValor, valorFinanciado, dadosCalculo);
    }

    private SimulacaoParcResidual criarSimulacao(SimulacaoParcResidualRequest request, List<Prazo> prazos,
                                                 LimitesSubsidioValor limitesSubsidioValor, BigDecimal valorFinanciado, DadosCalculo dadosCalculo) {
        SimulacaoParcResidual simulacao = new SimulacaoParcResidual(calculadoraServices, limitesSubsidioValor, featureToggleConfig, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
        simulacao.init(request, dadosCalculo, valorFinanciado, prazos);
        return simulacao;
    }

    public List<Prazo> carregarPrazos(SimulacaoParcResidualRequest request) {
        List<Prazo> prazos;
        Collection<Integer> meses = new ArrayList<>();
        if (request.getParcelas() != null) {
            meses = Arrays.asList(request.getParcelas());
        }

        Integer prazoDesejado = null;
        if (request.getPrazoDesejado() != null &&
                request.getTaxaSubsidio() != null &&
                request.getTaxaSubsidio().compareTo(BigDecimal.ZERO) > 0 &&
                request.getParcelas() != null &&
                request.getParcelas().length > 1) {
            prazoDesejado = request.getPrazoDesejado();
        }

        prazos = simulacaoServices.getParcelas(request.getPlanoId(), request.getPercentualEntrada(), meses,
                request.getTaxaMes(), prazoDesejado);
        return prazos;
    }

    private void carregarDadosDoPlano(SimulacaoParcResidualRequest request) {
        if (planoService != null) {
            this.plano = getPlanoByToggle(request.getPlanoId());
            request.setModalidade(Modalidade.fromValue(plano.getCodigoModalidade()));
            request.setPeriodicidade(plano.getPeriodicidadeNumero());
            request.setPermiteBalao(plano.getPermiteBalao());
            request.setControleBalao(plano.getControleBalao());
            request.setBalaoUltima(plano.getBalaoUltima());

            // BTO-1501: Se no plano q veio do Mongo houver parcelas intermediarias e permitir balao opcional
            if (plano.getParcelasIntermediarias() != null && simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request)) {
                request.setQuantidadeIntermediarias(plano.getParcelasIntermediarias().getQuantidadeMaximaBalao());
                request.setIntervaloResidual(plano.getParcelasIntermediarias().getIntervaloResidual());
                request.setRestricoesPercentuaisBalao(plano.getParcelasIntermediarias().getRestricoesPercentuaisBalao());
                request.setPercentuaisBalao(plano.getParcelasIntermediarias().getPercentuaisBalao());
            }
        } else {
            if (request.getModalidade() == null) {
                request.setModalidade(Modalidade.CICLO_TOYOTA);
            }
        }
    }

    private void validarValoresEntradaIntermediarias(SimulacaoParcResidualRequest request) {
        // Verifica se o valor de todas parcelas intermediarias SOMADAS extrapolam o valor do bem
        if (!request.getIsSimulacaoInterna()) {
            BigDecimal totalIntermediarias = BigDecimal.ZERO; // Somatorio do valor das intermediarias


            // Fluxo original para Ciclo Toyota CASO não permita balão opcional.
            if (request.getValorParcelaResidual() != null && !featureToggleConfig.getCicloIntermediariasEnabled()) {
                // No Ciclo Toyota original não existem parcelas intermediárias opcionais,
                // e portanto a ÚNICA parcela que é considerada no somatório é o residual.
                totalIntermediarias = request.getValorParcelaResidual();
            } else if (request.getIntermediarias() != null) {
                totalIntermediarias = request.getIntermediarias().stream().map(ParcelaIntermediaria::getValor)
                        .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
            }


            if (request.getValorEntrada().add(totalIntermediarias).compareTo(request.getValorBem()) > 0) {
                throw new BusinessValidationException("valor-entrada", request.getValorEntrada().toString(),
                        "Valor da entrada muito alto. Entrada mais valor para a "
                                + "parcela residual/intermediárias não pode ser maior que o valor do bem.");
            }
        }

    }

    /**
     * Valida se o valor da parcela residual respeita os ranges de percentuais de balão.
     *
     * @param request
     */
    private void validarValorResidualNoRangeBalao(SimulacaoParcResidualRequest request) {
        // Primeiro temos que considerar o valor do bem tendo a entrada sido abatida dele.
        BigDecimal valorBemMenosEntrada = request.getValorBem().subtract(request.getValorEntrada());

        // O valor da parcela residual nao pode ultrapassar o valor do bem abatido da entrada.
        if (request.getValorParcelaResidual().compareTo(valorBemMenosEntrada) >= 0) {
            throw new BusinessValidationException("valor-parcela-residual", request.getValorParcelaResidual().toString(),
                    "Valor residual muito alto. Valor residual nao pode ultrapassar o valor do bem abatido da entrada.");
        }

        Optional<PercentualBalao> percentualDesejado = request.getPercentuaisBalao()
                .stream()
                .filter(percentual -> request.getPrazoDesejado() >= percentual.getPrazoInicial() && request.getPrazoDesejado() <= percentual.getPrazoFinal())
                .findFirst();

        // Para o valor minimo e maximo, considerar o valor bem inteiro, sem descontar dele a entrada.
        if (percentualDesejado.isPresent()) {
            BigDecimal valorMinimo = request.getValorBem().multiply(percentualDesejado.get().getMinimo()).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);
            BigDecimal valorMaximo = request.getValorBem().multiply(percentualDesejado.get().getMaximo()).divide(NumberUtils.CEM, 2, RoundingMode.HALF_UP);

            if (request.getValorParcelaResidual().compareTo(valorMinimo) < 0 ||
                    request.getValorParcelaResidual().compareTo(valorMaximo) > 0) {
                throw new BusinessValidationException("valor-parcela-residual", request.getValorParcelaResidual().toString(),
                        "Valor residual esta fora do intervalo de percentuais do balao.");
            }
        }
    }

    private void corrigirParcelaResidual(SimulacaoParcResidualRequest request) {
        if (request.getControleBalao() == EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA && request.getValorParcelaDesejada() == null) {
            List<ParcelaIntermediaria> intermediarias = request.getIntermediarias();

            if (simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request)) {
                if (intermediarias == null) {
                    throw new BusinessValidationException("parcelas-intermediarias", null,
                            "Intermediária na ultima parcela é obrigatório");
                }

                // BTO-1501: Garantir que a parcela obtida da lista de intermediarias é a residual.
                //           Para tanto considerar a parcela cuja data de vencimento é o maior.
                Comparator<ParcelaIntermediaria> comparador = Comparator.comparing(ParcelaIntermediaria::getVencimento);
                ParcelaIntermediaria parcelaIntermediariaResidual = intermediarias.stream().filter(p -> p.getVencimento() != null).max(comparador).orElse(null);

                if (parcelaIntermediariaResidual != null)
                    request.setValorParcelaResidual(parcelaIntermediariaResidual.getValor());

                // TODO: analisar se devemos mandar na request a lista completa,
                //  incluindo residual, ou se manda somente a lista SEM o residual
                request.setIntermediarias(intermediarias);

                // Fluxo original caso não permita balão opcional
            } else if (request.getValorParcelaResidual() == null &&
                    intermediarias != null &&
                    intermediarias.size() == 1 &&
                    request.getModalidade() == Modalidade.CICLO_TOYOTA) {
                ParcelaIntermediaria intermediaria = intermediarias.get(0);
                request.setValorParcelaResidual(intermediaria.getValor());
                request.setIntermediarias(null);

            }

            if (intermediarias != null && request.getQuantidadeIntermediarias() != null && intermediarias.size() > request.getQuantidadeIntermediarias()) {
                throw new BusinessValidationException("parcelas-intermediarias", null,
                        "Configuração do plano excedeu o máximo permitido para intermediárias. A quantidade máxima de intermediárias permitido é de " + request.getQuantidadeIntermediarias() + " parcelas");
            }
        }
    }

    /**
     * Esse método deve ser chamado após o método corrigirParcelaResidual. O
     * objetivo é verificar se a modalidade está correta.
     */
    private void validarModalidadeNaSimulacao(SimulacaoParcResidualRequest request, List<Prazo> prazos) {
        List<ValidadorBalaoModalidade> validadores = new ArrayList<>();

        if (request.getModalidade() == Modalidade.CDC) {
            validadores.add(new ValidadorBalaoCDC(request, prazos));
        } else if (request.getModalidade() == Modalidade.CICLO_TOYOTA) {
            if (simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request)) {
                validadores.add(new ValidadorIntermediariasCiclo(request, prazos));
            } else {
                request.setIntermediarias(null);

                // A validação abaixo só deve ocorrer quando a simulação for modalidade CICLO-TOYOTA e tipo RESIDUAL.
                if (request.getValorParcelaDesejada() == null) {
                    validadores.add(new ValidadorBalaoCiclo(request.getIntermediarias(), request.getValorParcelaResidual(),
                            request.getControleBalao(), request.getPermiteBalao()));
                }
            }
        }

        ExecutorValidadores.executarValidadoresBalao(validadores);
    }

    protected LimitesSubsidioValor validarSubsidios(SimulacaoParcResidualRequest request, List<Prazo> prazos,
                                                    boolean corrigirMaximo, LocalDate dataCalculo) {

        Subsidio subsidio = validarSubsidio(request, prazos, corrigirMaximo);

        if (subsidio == null) {
            if (request.getRetorno() == null) {
                throw new BusinessValidationException("retorno", null, "Campo obrigatório para plano sem subsídio");
            }
            if (request.getValorSubsidio() != null) {
                throw new BusinessValidationException(VALOR_SUBSIDIO, request.getValorSubsidio().toString(),
                        "plano selecionado não possui subsídio");
            }
        } else {
            LimitesSubsidioValor limitePorPrazo = validarParametrosSubsidio(request, subsidio, corrigirMaximo,
                    dataCalculo, prazos);
            if (limitePorPrazo != null)
                return limitePorPrazo;
        }

        return new LimitesSubsidioValor(null, subsidio);
    }

    protected Subsidio validarSubsidio(SimulacaoRequest request, Collection<Prazo> prazos, boolean corrigirMaximo) {

        Subsidio subsidio = simulacaoServices.getSubsidio(request.getPlanoId());
        if (subsidio != null && subsidio.getTipoSubsidio() == EnumTipoSubsidio.FIXO) {
            if (request.getTaxaSubsidio() != null) {
                throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
                        "plano selecionado é do tipo subsídio fixo");
            }
            // a taxa do base_plano_taxas pode estar fora dos limites, então vamos corrigir,
            // mesmo porque a taxa
            // nem é passada
            corrigirMaximo = true;
            request.setTaxaSubsidio(subsidio.getTaxaBanco());
        }

        // A classe ServicesSimulacao (que não é parte do projeto) só usa a taxa ou
        // valor do subsídio se o código
        // de retorno for "S", o que me soa estranho. Além disso o valor do retorno
        // passado para a planilha é zero.
        // O correto seria passar null nesses dois campos caso não haja subsídio e nesse
        // caso setar o valor do
        // retorno para zero.
        if (request.getTaxaSubsidio() != null) {
            if (subsidio == null) {
                throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
                        "plano selecionado não possui subsídio");
            }
            request.setRetorno("S");
            BigDecimal maxTaxaSubsidio = getMaxTaxaSubsidio(prazos);
            if (!corrigirMaximo && maxTaxaSubsidio.compareTo(request.getTaxaSubsidio()) < 0
                    || request.getTaxaSubsidio().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessValidationException(TAXA_SUBSIDIO, request.getTaxaSubsidio().toString(),
                        "A taxa máxima para subsídio é de " + maxTaxaSubsidio.setScale(2, RoundingMode.HALF_UP) + " %");
            }
        }
        return subsidio;
    }

    private LimitesSubsidioValor validarParametrosSubsidio(SimulacaoParcResidualRequest request, Subsidio subsidio,
                                                           boolean corrigirMaximo, LocalDate dataCalculo, List<Prazo> prazos) {
        if (subsidio.getTipoSubsidio() == EnumTipoSubsidio.FIXO && request.getValorSubsidio() != null) {
            throw new BusinessValidationException(VALOR_SUBSIDIO, request.getValorSubsidio().toString(),
                    "plano selecionado é do tipo subsídio fixo");
        }
        if (request.getValorSubsidio() != null) {
            request.setRetorno("S");
            Map<Integer, BigDecimal> limitePorPrazo = getValorMaximoDoSubsidioImpl(request, dataCalculo, prazos);
            BigDecimal maxValorSubsidio = limitePorPrazo.values().stream().max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            BigDecimal valorDoSubsidio = request.getValorSubsidio();
            if (maxValorSubsidio.compareTo(valorDoSubsidio) < 0
                    || valorDoSubsidio.compareTo(subsidioMinimoPorValor) < 0) {
                if (corrigirMaximo && maxValorSubsidio.compareTo(valorDoSubsidio) < 0) {
                    request.setValorSubsidio(maxValorSubsidio);
                } else {
                    if (!request.getIsSimulacaoInterna()) { // INC0047473/PRB0041148 -> Não dar erro se for simulação
                        // interna
                        if (subsidio.getTipoSubsidio() != null && subsidio.getTipoSubsidio().equals(EnumTipoSubsidio.VARIAVEL) &&
                                subsidio.getTipoDistribuicao() != null && subsidio.getTipoDistribuicao().equals(EnumTipoDistribuicao.FAIXA_VALOR) &&
                                subsidio.getFaixasValor() != null
                        ) {
                            Optional<Subsidio.FaixaValor> optionalMinValor = subsidio.getFaixasValor().stream().min(Comparator.comparing(Subsidio.FaixaValor::getValorInicial));
                            Optional<Subsidio.FaixaValor> optionalMaxValor = subsidio.getFaixasValor().stream().max(Comparator.comparing(Subsidio.FaixaValor::getValorFinal));

                            throw new BusinessValidationException(VALOR_SUBSIDIO, valorDoSubsidio.toString(),
                                    "O valor do subsídio utilizado respeita as faixas de valor, que vão de R$ " + NumberUtils.format(optionalMinValor.get().getValorInicial()) + " a R$ "
                                            + NumberUtils.format(optionalMaxValor.get().getValorFinal()) + ", contudo o valor máximo de subsídio calculado internamente só permite valores de R$" + NumberUtils.format(subsidioMinimoPorValor) + " a R$" + NumberUtils.format(maxValorSubsidio) + ". Escolha um valor de subsídio menor.");
                        } else {
                            throw new BusinessValidationException(VALOR_SUBSIDIO, valorDoSubsidio.toString(),
                                    "O valor do subsídio é de R$ " + NumberUtils.format(subsidioMinimoPorValor) + " a R$ "
                                            + NumberUtils.format(maxValorSubsidio));
                        }
                    }

                }
            }
            if (request.getTaxaSubsidio() != null) {
                throw new BusinessValidationException(VALOR_SUBSIDIO, valorDoSubsidio.toString(),
                        "valor-subsidio não pode ser usado quando taxa-subsidio for usado");
            }
            return new LimitesSubsidioValor(limitePorPrazo, subsidio);
        } else if (request.getTaxaSubsidio() == null) {
            throw new BusinessValidationException(PLANO_ID, request.getPlanoId().toString(),
                    "plano com subsídio requer valor-subsidio ou taxa-subsidio");
        }
        return null;
    }

    protected BigDecimal getMaxTaxaSubsidio(Collection<Prazo> prazos) {
        return prazos.stream().map(Prazo::getTaxaBanco).max(Comparator.naturalOrder())
                .orElseThrow(() -> new RuntimeException(
                        "Nunca vai passar aqui, mas temos que colocar esse orElseThrow pra calar a boca do Sonar"))
                .subtract(new BigDecimal("0.01"));
        // o motivo é simples, no método que carrega a lista de prazos já fazemos a
        // validação pra garantir que não é uma lista vazia
    }

    private Plano getPlanoByToggle(Integer planoId) {
        Plano plano;
        if (featureToggleConfig.getBuscaApiMotorTaxaEnabled()) {
            PlanoMotorTaxa planoResponse = this.motorTaxaPlanoService.getPlanoPorId(planoId);
            plano = this.motorTaxaPlanoService.convertPlanoFromMotorTaxa(planoResponse);
        } else {
            plano = this.planoService.getPlanoById(planoId);
        }
        return plano;
    }

    private void corrigirDataCalculo(SimulacaoParcResidualRequest request, LocalDate dataCalculo) {
        if (Objects.isNull(dataCalculo) && Objects.isNull(request.getDataPrimeiroVencimento()) && Objects.isNull(request.getDataCalculo())) {
            request.setDataCalculo(request.getDataCalculo());
        }

        if (Objects.isNull(request.getDataCalculo()) && Objects.nonNull(dataCalculo)) {
            request.setDataCalculo(dataCalculo);
        } else {
            request.setDataCalculo(request.getDataCalculo());
        }
    }

}