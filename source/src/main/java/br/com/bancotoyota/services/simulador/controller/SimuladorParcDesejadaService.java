package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.TipoDeSimulacao;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * O simulador simplificado usava o SimuladorParcDesejadaController, no entanto esse bean não vai mais subir por
 * conta do novo simulador pela parcela desejada. Por esse motivo criamos essa cópia da classe para ser usado
 * pelo simulador simplificado. Logo apagaremos o essa classe e o SimuladorParcDesejadaController e teremos
 * que mudar o simulador simplificado para usar o novo simulador.
 */
@Slf4j
@Service
public class SimuladorParcDesejadaService extends SimuladorExceptionController {

    private static final BigDecimal DEZ_MIL = new BigDecimal(10000);
    private static final int MAX_ITERACOES = 100;
    protected static final BigDecimal PRECISAO = new BigDecimal("0.10");
    public static final String PARCELA_RESIDUAL = " - % parcela residual: ";
    public static final String PRAZO = " prazo: ";
    private static FluxoService fluxoService = FluxoService.getInstance();

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private LogService logService;

    @Value("${simulador.max-requests-externos}")
    private int maxRequestsExternos;

    @Value("${simulador.desejada.max-diferenca}")
    private BigDecimal limiteDiferencaEmReais = new BigDecimal(5);

    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;

    CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    @Autowired
    public SimuladorParcDesejadaService(CalculadoraServices calculadoraService,
                                        SimulacaoServices simulacaoService, DataCarencia dataCarencia,
                                        ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService,
                                        @Value("${simulador.debug.retornar-request}") boolean debugRetornarRequest,
                                        SeguroPrestamistaPlusServiceImpl prestamistaService, CalculadoraSeguroFranquiaServices seguroFranquiaServices,  CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        super(calculadoraService, simulacaoService, dataCarencia, parametrosDeSeguroPrestamistaService, debugRetornarRequest,prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
    }

    /**
     * Método usado apenas em testes onde não precisamos passar o parâmetro headers.
     */
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(SimulacaoParcDesejadaRequest request) {
        return getParcelaDesejada(request, null);
    }

    public SimulacaoParcDesejadaResponse getParcelaDesejadaImpl(SimulacaoParcDesejadaRequest request) {
        // Passamos a flag corrigirMaximo como true porque pode ser que o novo plano não aceite a taxa que era
        // usada no plano anterior.
        return getParcelaDesejada(null, request, true, createDadosCalculo(request));
    }

    /**
     * Metodo para simulação de parcela desejada. Funciona apenas para planos ciclo-toyota.
     *
     * @param request
     * @return Lista com os 10 melhores resultados dos numeros de parcela,
     * valores e parcela residual com a porcentagem
     * @throws Exception
     */
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(SimulacaoParcDesejadaRequest request, HttpHeaders headers) {

        logHeaders(headers);
        try {
            ResponseEntity<SimulacaoParcDesejadaResponse> result = ResponseEntity.ok(
                getParcelaDesejada(null, request, false, createDadosCalculo(request)));
            // retornamos o request somente para simulação interna
            result.getBody().setRequest(null);
            // Alguns testes do simulador simplificado estão comparando o campo resultadoCalculado mesmo ele estando
            // marcado com JsonIgnore. Assim setamos ele pra null pra não atrapalhar o teste
            result.getBody().getPrazos().forEach(p -> p.setResultadoCalculado(null));
            return result;
        } finally {
            finalizarRequest();
        }
    }

    private void logHeaders(HttpHeaders headers) {
        if (logService != null) {
            logService.logHeaders(headers);
        }
        if (headers != null) {
            limitarRequestsExternos(headers.get("forwarded"), maxRequestsExternos);
        }
    }

    private DadosCalculo createDadosCalculo(SimulacaoParcDesejadaRequest request) {
    	String cnpjOrigemNegocio = null;
		List<Integer> segurosPrestamistasExcluidos = null;
        boolean removerTodosSPF = false;
        boolean removerTodosSVP = false;

		if (ObjectUtils.isNotEmpty(request.getParametrosDeSeguroPrestamista())) {
			cnpjOrigemNegocio = request.getParametrosDeSeguroPrestamista().getCnpjOrigemNegocio();
			segurosPrestamistasExcluidos =request.getParametrosDeSeguroPrestamista().getSegurosPrestamistasExcluidos();
            removerTodosSPF = request.getParametrosDeSeguroPrestamista().isSpfRemovido();
            removerTodosSVP = request.getParametrosDeSeguroPrestamista().isSvpRemovido();
		}

        return new DadosCalculo(getDataCalculo(request),
                new IOF(simulacaoServices.getTaxasIOF(request.getTipoPessoaEnum(), request.getIsentaIOF())),
                prestamistaService.getSegurosPrestamista(cnpjOrigemNegocio, dataCarencia.getDataCalculo(), segurosPrestamistasExcluidos, removerTodosSPF, removerTodosSVP),
                request.calcularValorBaseParaCalculoDoSeguro(), null, null, null);
    }

    /**
     * Esse request é apenas interno, não sendo necessário atualizar o Swagger. É chamado pelo serviço de
     * propostas para a orquestração: carregar os dados para abrir na tela.
     */
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(String token, SimulacaoParcDesejadaRequest request) {
        return ResponseEntity.ok(getParcelaDesejada(token, request, true, createDadosCalculo(request)));
    }

    private SimulacaoParcDesejadaResponse getParcelaDesejada(String token, SimulacaoParcDesejadaRequest request,
                                             boolean corrigirMaximo, DadosCalculo dadosCalculo) {

    	StopWatch sw = new StopWatch();

    	if (request.getModalidade() == Modalidade.CDC) {
    	    throw new BusinessValidationException("simulação pela parcela desejada só deve ser usada com planos " +
                    "ciclo-toyota pois funciona através da variação do residual");
        }

    	log.debug("Iniciando simulação da proposta:" + LocalDateTime.now());
		sw.start("carregarParametrosSeguroPrestamista");
		log.debug(" Inicio -> carregarParametrosSeguroPrestamista");
        carregarDadosDoProponente(token, request);
        sw.stop();
    	log.debug(" Fim -> carregarParametrosSeguroPrestamista");

    	// fazemos o log do request depois de atualizar os dados do proponente pra facilitar análise caso necessário
        if (logService != null) {
            logService.logData(request, "parâmetros para parcela desejada: ");
        }

        sw.start("isDentroCarencia");
        dataCarencia.isDentroCarencia(request, dadosCalculo.getDataCalculo()); // Valida a data do primeiro vencimento
        sw.stop();
        
        sw.start("filtrarPrazos");
        log.debug(" Inicio -> filtrarPrazos");
        Map<Integer, Prazo> prazosMap = filtrarPrazos(request);
        sw.stop();
    	log.debug(" Fim -> filtrarPrazos");

    	sw.start("validarSubsidio");
        log.debug(" Inicio -> validarSubsidio");
        Subsidio subsidio = validarSubsidio(request, corrigirMaximo, prazosMap);
        sw.stop();
    	log.debug(" Fim -> validarSubsidio");

        log.debug(" Inicio -> getParcelaDesejada");
        List<ResultadoCalculadoParcelaDesejada> resultados =
                getParcelaDesejada(request, prazosMap, dadosCalculo, sw);
    	log.debug(" Fim ->getParcelaDesejada");

    	sw.start("calcularEPopularValoresNaParcela");
        log.debug(" Inicio -> calcularEPopularValoresNaParcela");
        calcularEPopularValoresNaParcela(request, resultados, prazosMap, dadosCalculo);
        sw.stop();
    	log.debug(" Fim ->calcularEPopularValoresNaParcela");

    	sw.start("ordenar");
        log.debug(" Inicio -> ordenar");
        resultados.sort(Comparator.comparing(ResultadoCalculadoParcelaDesejada::getPrazo));
        sw.stop();
    	log.debug(" Fim ->ordenar");

        SimulacaoParcDesejadaResponse response =
                new SimulacaoParcDesejadaResponse(resultados, subsidio, dadosCalculo.getDataCalculo(), request,
                        limiteDiferencaEmReais, debugRetornarRequest);
        if (token != null && logService != null) {
            // geramos o log só para o caso de simulação interna onde os dados não estão disponíveis no navegador
            logService.logData(response, "resposta parcela desejada");
        }
        
        log.debug(" Tempo-> rotina parcela desejada -> " + sw.prettyPrint());
    	log.debug("Fim simulação da proposta:" + LocalDateTime.now());
        return response;
    }

    /**
     *  Aqui é o coração da simulação pela parcela desejada.
     */
    private List<ResultadoCalculadoParcelaDesejada> getParcelaDesejada(SimulacaoParcDesejadaRequest request,
                                       Map<Integer, Prazo> prazosMap, DadosCalculo dadosCalculo, StopWatch sw) {

    	sw.start("calcular residuais");
        log.debug(" Inicio -> getParcelaDesejada.calcularResiduais");

        // aproximação inicial com variação de 1% do residual
        Map<Integer, ResultadoCalculadoParcelaDesejada> resultadosParciais =
                calcularResiduais(request, prazosMap, dadosCalculo);
        
        sw.stop();
    	log.debug(" Fim -> getParcelaDesejada.calcularResiduais");

    	sw.start("refinar resultados");
        log.debug(" Inicio -> getParcelaDesejada.refinarResultados");
    	List<ResultadoCalculadoParcelaDesejada> resultados = refinarResultados(request, prazosMap, resultadosParciais, dadosCalculo);
        sw.stop();
      	log.debug(" Fim -> getParcelaDesejada.refinarResultados");

        return resultados;
    }

    private Subsidio validarSubsidio(SimulacaoParcDesejadaRequest request, boolean corrigirMaximo, Map<Integer, Prazo> prazosMap) {
        Subsidio subsidio = validarSubsidio(request, prazosMap.values(), corrigirMaximo);
        if (subsidio != null) {
            if (request.getTaxaSubsidio() == null) {
                throw new BusinessValidationException("taxa-subsidio", null, "Campo obrigatório para plano com subsídio");
            }
        } else {
            if (request.getRetorno() == null) {
                throw new BusinessValidationException("retorno", null, "Campo obrigatório para plano sem subsídio");
            }
        }
        return subsidio;
    }

    private Map<Integer, Prazo> filtrarPrazos(SimulacaoParcDesejadaRequest request) {
        return getPrazosMap(request.getPlanoId(), request.getPercentualEntrada(),
                request.getParcelas() == null ? null : Arrays.asList(request.getParcelas()),
                request.getTaxaMes());
    }

    /**
     * Para cada prazo variamos o residual com degraus de 1% começando no valor máximo (isto é com a menor parcela
     * para aquele prazo) até chegar no valor mínimo do residual ou até conseguir um valor de parcela que seja
     * maior que o valor desejado. No próximo passo faremos o refinamento em torno dos valores obtidos aqui.
     */
    protected Map<Integer, ResultadoCalculadoParcelaDesejada> calcularResiduais(SimulacaoParcDesejadaRequest request,
                                      Map<Integer, Prazo> prazosMap, DadosCalculo dadosCalculo) {

        Map<Integer, ResultadoCalculadoParcelaDesejada> resultadosParciais = new TreeMap<>();
        // calcula o valor máximo para o residual de acordo com o valor da entrada
        StopWatch sw = new StopWatch();
    	sw.start();
        log.debug(" Inicio -> getParcelaDesejada.calcularResiduais.getMaxPercentualParcelaResidual");
        BigDecimal percentualParcelaResidualInicial = getMaxPercentualParcelaResidual(request);
        sw.stop();
    	log.debug(" Fim -> getParcelaDesejada.calcularResiduais.getMaxPercentualParcelaResidual");

    	log.debug(" Tempo-> getParcelaDesejada.calcularResiduais.getMaxPercentualParcelaResidual -> " + sw.toString());
        BigDecimal percentualParcelaResidual = percentualParcelaResidualInicial;
        BigDecimal maxPercentualParcelaResidualAnterior = null;

        sw = new StopWatch();
    	sw.start();
        log.debug(" Inicio -> getParcelaDesejada.calcularResiduais.ordenarPrazos");
    
        // os prazos devem ser processados em ordem
        List<Prazo> list = new ArrayList<>(prazosMap.values());
        list.sort(Comparator.comparing(Prazo::getNumeroDeParcelas));
        
        sw.stop();
    	log.debug(" Fim -> getParcelaDesejada.calcularResiduais.ordenarPrazos");

    	log.debug(" Tempo-> getParcelaDesejada.calcularResiduais.ordenarPrazos -> " + sw.toString());

    	sw = new StopWatch();
       	sw.start();
        log.debug(" Inicio -> getParcelaDesejada.calcularResiduais.validaPercentualInicial");
       
        validaPercentualInicial(percentualParcelaResidual, list, request.getValorEntrada());
        
        sw.stop();
    	log.debug(" Fim -> getParcelaDesejada.calcularResiduais.validaPercentualInicial");

    	log.debug(" Tempo-> getParcelaDesejada.calcularResiduais.validaPercentualInicial-> " + sw.toString());

    	
    	sw = new StopWatch();
       	sw.start();
        log.debug(" Inicio -> getParcelaDesejada.calcularResiduais.loop prazos");
        int totalCount = 0;
        for (Prazo prazo : list) {
            BigDecimal maxPercentualParcelaResidual = prazo.getPercentualMaxBalao();

            PercentualParcelaResidual ppr = determinaResidualInicialParaPrazo(
                    maxPercentualParcelaResidualAnterior, maxPercentualParcelaResidual, percentualParcelaResidual, percentualParcelaResidualInicial);
            
            percentualParcelaResidual = ppr.getPercentual();
            maxPercentualParcelaResidualAnterior = ppr.getMaxPercentualParcelaResidualAnterior();

            if (percentualParcelaResidual.compareTo(maxPercentualParcelaResidual) > 0) {
                percentualParcelaResidual = maxPercentualParcelaResidual;
            }
            BigDecimal minPercentualParcelaResidual = prazo.getPercentualMinBalao();
            ResultadoCalculadoParcelaDesejada resultado = null;

            // adiciona 1 pois na primeira execução será removido 1
            percentualParcelaResidual = percentualParcelaResidual.add(BigDecimal.ONE);

            int count = 0;
            // Faz um loop começando com a maior parcela residual até a menor de 1% em 1%.
            
            do {
                percentualParcelaResidual = subtraiUm(percentualParcelaResidual, minPercentualParcelaResidual);
                log.trace("prazo: " + prazo.getNumeroDeParcelas() + PARCELA_RESIDUAL + percentualParcelaResidual);
                count ++;
                StopWatch sw2 = new StopWatch();
                sw2.start();
                log.debug(" Inicio -> getParcelaDesejada.calcularResiduais.loop prazos.do while " + PRAZO + prazo.getNumeroDeParcelas() + PARCELA_RESIDUAL + percentualParcelaResidual);
   
                ResultadoCalculadoParcelaDesejada candidato = calcularCandidato(request, prazosMap,
                        percentualParcelaResidual, prazo, request.getIdade(), dadosCalculo);
                candidato.setMinPercResidual(minPercentualParcelaResidual);
                candidato.setMaxPercResidual(maxPercentualParcelaResidual);
                
                sw2.stop();
                log.debug(" Fim -> getParcelaDesejada.calcularResiduais.loop prazos.do while " + PRAZO + prazo.getNumeroDeParcelas() + PARCELA_RESIDUAL + percentualParcelaResidual);

            	log.debug(" Tempo -> getParcelaDesejada.calcularResiduais.loop prazos.do while " + PRAZO + prazo.getNumeroDeParcelas() + PARCELA_RESIDUAL + percentualParcelaResidual + " ->" +  sw2.toString());

            	
                if (candidato.getValorParcela().compareTo(request.getValorParcelaDesejada()) > 0) {
                    if (resultado == null) {
                        // se o primeiro valor de parcela já é maior que a parcela desejada então nem precisa continuar
                        // pois o valor da parcela só vai aumentar ao se diminuir a parcela residual
                        resultado = candidato;
                    }
                    break;
                } else {
                    resultado = candidato;
                }
            } while (percentualParcelaResidual.compareTo(minPercentualParcelaResidual) > 0);
            
            resultadosParciais.put(prazo.getNumeroDeParcelas(), resultado);
            log.debug("interações para prazo " + prazo.getNumeroDeParcelas() + ": " + count);
            totalCount += count;
        }
        log.info("total de simulações iniciais: " + totalCount);
        
        sw.stop();
    	log.debug(" Fim ->  getParcelaDesejada.calcularResiduais.loop prazos");

    	log.debug(" Tempo->  getParcelaDesejada.calcularResiduais.loop prazos-> " + sw.toString());
    	
        return resultadosParciais;
    }

    /**
     * Caso o valor máximo para o residual caia com o aumento do prazo podemos começar o novo prazo com o último
     * residual calculado. Com isso conseguimos reduzir o número de chamadas para a planilha.
     * Caso isso não ocorra usamos o valor inicial só para o prazo em que houve a mudança para mais.
     */
    private PercentualParcelaResidual determinaResidualInicialParaPrazo(
            BigDecimal maxPercentualParcelaResidualAnterior,
            BigDecimal maxPercentualParcelaResidualNoPrazo,
            BigDecimal percentualParcelaResidual,
            BigDecimal percentualParcelaResidualInicial) {
        if (maxPercentualParcelaResidualAnterior == null) {
            maxPercentualParcelaResidualAnterior = maxPercentualParcelaResidualNoPrazo;
        } else {
            // caso haja aumento do máximo para a parcela residual devemos recomeçar a parcela residual com o valor inicial
            if (maxPercentualParcelaResidualAnterior.compareTo(maxPercentualParcelaResidualNoPrazo) < 0) {
                percentualParcelaResidual = percentualParcelaResidualInicial;
            }
            maxPercentualParcelaResidualAnterior = maxPercentualParcelaResidualNoPrazo;
        }
        return new PercentualParcelaResidual(percentualParcelaResidual, maxPercentualParcelaResidualAnterior);
    }

    private void validaPercentualInicial(BigDecimal percentualParcelaResidual, List<Prazo> list, BigDecimal entrada) {
        list.stream().map(Prazo::getPercentualMinBalao).min(BigDecimal::compareTo).ifPresent(min -> {
            if (min.compareTo(percentualParcelaResidual) > 0) {
                throw new BusinessValidationException("valor-entrada", entrada.toString() ,"Valor da entrada muito alto. Entrada mais valor mínimo para a " +
                        "parcela residual não pode ser maior que o valor do bem.");
            }
        });
    }

    private BigDecimal subtraiUm(BigDecimal percentualParcelaResidual, BigDecimal minPercentualParcelaResidual) {
        percentualParcelaResidual = percentualParcelaResidual.subtract(BigDecimal.ONE);
        if (percentualParcelaResidual.compareTo(minPercentualParcelaResidual) < 0) {
            percentualParcelaResidual = minPercentualParcelaResidual;
        }
        return percentualParcelaResidual;
    }

    private ResultadoCalculadoParcelaDesejada calcularCandidato(SimulacaoParcDesejadaRequest request,
                                                                Map<Integer, Prazo> prazosMap,
                                                                BigDecimal percentualParcelaResidual,
                                                                Prazo prazo,
                                                                Integer idade, DadosCalculo dadosCalculo) {


        BigDecimal valorParcelaResidual = request.getValorBem().multiply(
                    percentualParcelaResidual.divide(NumberUtils.CEM)).setScale(2, RoundingMode.HALF_UP);

        if (valorParcelaResidual.compareTo(BigDecimal.ZERO) == 0) {
            // AutBank não aceita residual zero
            valorParcelaResidual = UM_CENTESIMO;
        }
        CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista = CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
                dadosCalculo, request.getParametrosDeSeguroPrestamista(), dadosCalculo.getValorBaseParaCalculoDoSeguro(),
                null, idade, request.getTipoPessoaEnum());
        SeguroFranquia seguroFranquia = new SeguroFranquia();
        seguroFranquia.setValorTotal(request.getVlrSeguroFranquia());
        seguroFranquia.setValorParcela(BigDecimal.ZERO);
        BigDecimal valorDoSeguroPrestamista = custoPercentualDoSeguroPrestamista.getFator().multiply(dadosCalculo.getValorBaseParaCalculoDoSeguro());
        ResultadoCalculado resultadoCalculado = calcular(request, valorParcelaResidual, prazosMap, prazo.getNumeroDeParcelas(), valorDoSeguroPrestamista, dadosCalculo);
        valorParcelaResidual = new BigDecimal(resultadoCalculado.getDadosSimulacao().getParcelasBalao().get(0).getValorParcela());

        BigDecimal valorParcela = fluxoService.getValorDaParcela(resultadoCalculado);

        CustoPercentualDoSeguroPrestamista novoCustoPercentualDoSeguroPrestamista =
                CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(dadosCalculo, request.getParametrosDeSeguroPrestamista(),
                    dadosCalculo.getValorBaseParaCalculoDoSeguro(), valorParcela, idade, request.getTipoPessoaEnum());

        if (custoPercentualDoSeguroPrestamista.getTipo() != novoCustoPercentualDoSeguroPrestamista.getTipo()) {

            custoPercentualDoSeguroPrestamista = novoCustoPercentualDoSeguroPrestamista;
            valorDoSeguroPrestamista = custoPercentualDoSeguroPrestamista.getFator().multiply(dadosCalculo.getValorBaseParaCalculoDoSeguro());

            resultadoCalculado = calcular(request, valorParcelaResidual, prazosMap, prazo.getNumeroDeParcelas(), valorDoSeguroPrestamista, dadosCalculo);
            valorParcelaResidual = new BigDecimal(resultadoCalculado.getDadosSimulacao().getParcelasBalao().get(0).getValorParcela());

            valorParcela = fluxoService.getValorDaParcela(resultadoCalculado);
        }

        return criarResultadoCalculadoParcelaDesejada(percentualParcelaResidual, prazo, valorParcelaResidual,
                custoPercentualDoSeguroPrestamista, valorDoSeguroPrestamista, seguroFranquia, resultadoCalculado, valorParcela, dadosCalculo);
    }

    private ResultadoCalculadoParcelaDesejada criarResultadoCalculadoParcelaDesejada(
            BigDecimal percentualParcelaResidual,
            Prazo prazo,
            BigDecimal valorParcelaResidual,
            CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista,
            BigDecimal valorDoSeguroPrestamista,
            SeguroFranquia seguroFranquia,
            ResultadoCalculado resultadoCalculado,
            BigDecimal valorParcela, DadosCalculo dadosCalculo) {
        BigDecimal valorIOF = NumberUtils.toBigDecimal(resultadoCalculado.getValorIOF());
        BigDecimal valorCetAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaCetAnual());
        BigDecimal valorFinanciado = NumberUtils.toBigDecimal(resultadoCalculado.getValorFinanciamento());
        BigDecimal valorTaxaMes = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaMes());
        BigDecimal valorTaxaAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaAnual());
        BigDecimal valorSubsidio = NumberUtils.toBigDecimal(resultadoCalculado.getValorSubsidio());
        BigDecimal valorFinanciamento = NumberUtils.toBigDecimal(resultadoCalculado.getValorTotalFinanciamento());

        BigDecimal taxaBancoMotorTaxas = resultadoCalculado.getValorTaxaBanco();

        return new ResultadoCalculadoParcelaDesejada(
                new ResultadoCalculadoParcelaResidual(
                    prazo.getNumeroDeParcelas(),
                    valorParcela,
                    valorIOF,
                    valorCetAnual,
                    valorFinanciado,
                    valorTaxaMes,
                    valorTaxaAnual,
                    valorSubsidio,
                    dadosCalculo.getSeguroGarantia(),
                    custoPercentualDoSeguroPrestamista,
                    valorDoSeguroPrestamista,
                    null,
                        seguroFranquia,
                    valorParcelaResidual,
                    null,
                    null,
                    null,
                    resultadoCalculado.getValorSubsidioUsado(),
                    resultadoCalculado.getTaxaSubsidioUsada(),
                    valorFinanciamento,
                    percentualParcelaResidual,
                    resultadoCalculado,
                    valorTaxaMes,
                    valorTaxaAnual,
                    resultadoCalculado.getValorSubsidioUsado(),
                        taxaBancoMotorTaxas,
                        null),
                null,
                null
                );
    }

    /**
     *
     */
    private List<ResultadoCalculadoParcelaDesejada> refinarResultados(SimulacaoParcDesejadaRequest request,
                                  Map<Integer, Prazo> prazosMap,
                                  Map<Integer, ResultadoCalculadoParcelaDesejada> resultadosParciais,
                                  DadosCalculo dadosCalculo) {
        List<ResultadoCalculadoParcelaDesejada> resultados = new ArrayList<>();
        for (Map.Entry<Integer, ResultadoCalculadoParcelaDesejada> prazo : resultadosParciais.entrySet()) {
            ResultadoCalculadoParcelaDesejada resultadoAnterior = prazo.getValue();

            BigDecimal percentualResidualMaximo;
            BigDecimal percentualResidualMinimo;
            if (resultadoAnterior.getValorParcela().compareTo(request.getValorParcelaDesejada()) > 0) {
                // Se o valor calculado até então é maior que o valor desejado então precisamos aumentar o residual.
                // O intervalo a ser considerado começará no valor atual do residual até 1% a mais
                percentualResidualMinimo = resultadoAnterior.getPercentualParcelaResidual();
                percentualResidualMaximo = percentualResidualMinimo.add(BigDecimal.ONE);
            } else {
                // se for menor então temos que diminuir o residual do valor atual até 1% a menos
                percentualResidualMaximo = resultadoAnterior.getPercentualParcelaResidual();
                percentualResidualMinimo = percentualResidualMaximo.subtract(BigDecimal.ONE);
            }

            // verificando se já chegamos nos limites do residual para o prazo
            if (percentualResidualMinimo.compareTo(resultadoAnterior.getMinPercResidual()) >= 0 &&
                    percentualResidualMaximo.compareTo(resultadoAnterior.getMaxPercResidual()) <= 0) {

                // se ainda estiver dentro dos limites então podemos fazer mais uma rodada de refinamento
                BigDecimal[] limitesDeResidual = new BigDecimal[] { percentualResidualMinimo, percentualResidualMaximo };
                ResultadoCalculadoParcelaDesejada resultado = refinarResultado(request, prazosMap,
                        prazo.getKey(), resultadoAnterior, limitesDeResidual,
                        request.getIdade(), dadosCalculo);
                resultados.add(resultado);
            } else {
                // senão o resultado será o resultado anterior
                resultados.add(resultadoAnterior);
            }
        }
        return resultados;
    }

    /**
     * Faz busca binária entre os percentuais mínimo e máximo do residual.
     */
    private ResultadoCalculadoParcelaDesejada refinarResultado(SimulacaoParcDesejadaRequest request,
                                                               Map<Integer, Prazo> prazosMap,
                                                               Integer prazo,
                                                               ResultadoCalculadoParcelaDesejada resultadoAnterior,
                                                               BigDecimal[] limitesDeResidual,
                                                               Integer idade, DadosCalculo dadosCalculo) {

        BigDecimal percentualResidualMinimo = limitesDeResidual[0];
        BigDecimal percentualResidualMaximo = limitesDeResidual[1];
        BigDecimal valorParcelaResidualAnterior = resultadoAnterior.getValorParcelaResidual();
        BigDecimal valorParcelaAnterior = resultadoAnterior.getValorParcela();

        BigDecimal percentualParcelaResidual;
        int count = 0;
        boolean repetir;
        ResultadoCalculadoParcelaDesejada resultado = resultadoAnterior;
        do {
            percentualParcelaResidual = calcularMetadeDoIntervalo(percentualResidualMaximo, percentualResidualMinimo);

            BigDecimal valorParcelaResidual = request.getValorBem().multiply(
                    percentualParcelaResidual.divide(NumberUtils.CEM)).setScale(2, RoundingMode.HALF_UP);

            if (valorParcelaResidual.compareTo(BigDecimal.ZERO) == 0) {
                // AutBank não aceita residual zero
                valorParcelaResidual = UM_CENTESIMO;
            }

            ResultadoCalculadoParcelaResidual resultadoCalculadoParcelaResidual = calcularComSeguro(request, prazosMap, prazo,
                    valorParcelaResidual, idade, dadosCalculo);

            BigDecimal valorParcela = resultadoCalculadoParcelaResidual.getValorParcela();

            if (valorParcela.compareTo(request.getValorParcelaDesejada()) > 0) {
                percentualResidualMinimo = percentualParcelaResidual;
            } else {
                resultado = criarResultado(prazo, percentualResidualMaximo, percentualResidualMinimo,
                        percentualParcelaResidual, valorParcelaResidual, resultadoCalculadoParcelaResidual, valorParcela, dadosCalculo);
                percentualResidualMaximo = percentualParcelaResidual;
            }

            BigDecimal diferencaValorParcela = valorParcelaAnterior.subtract(valorParcela).abs();
            BigDecimal diferencaValorParcelaResidual = valorParcelaResidualAnterior.subtract(valorParcelaResidual).abs();
            repetir = (diferencaValorParcela.compareTo(PRECISAO) > 0 &&
                    diferencaValorParcelaResidual.compareTo(PRECISAO) > 0);

            if (repetir) {
                valorParcelaAnterior = valorParcela;
                valorParcelaResidualAnterior = valorParcelaResidual;
                count++;
                if (count >= MAX_ITERACOES) {
                    repetir = false;
                }
            }
        } while (repetir);
        log.debug("interações para prazo " + prazo + ": " + count);
        return resultado;
    }

    private ResultadoCalculadoParcelaDesejada criarResultado(
            Integer prazo,
            BigDecimal percentualResidualMaximo,
            BigDecimal percentualResidualMinimo,
            BigDecimal percentualParcelaResidual,
            BigDecimal valorParcelaResidual,
            ResultadoCalculadoParcelaResidual resultadoCalculadoParcelaResidual,
            BigDecimal valorParcela, DadosCalculo dadosCalculo) {

        return new ResultadoCalculadoParcelaDesejada(
                new ResultadoCalculadoParcelaResidual(
                    prazo,
                    valorParcela,
                    resultadoCalculadoParcelaResidual.getValorIOF(),
                    resultadoCalculadoParcelaResidual.getValorCetAnual(),
                    resultadoCalculadoParcelaResidual.getValorTotalFinanciado(),
                    resultadoCalculadoParcelaResidual.getValorTaxaMes(),
                    resultadoCalculadoParcelaResidual.getValorTaxaAnual(),
                    resultadoCalculadoParcelaResidual.getValorSubsidio(),
                    resultadoCalculadoParcelaResidual.getDadosSeguroMecanica(),
                    resultadoCalculadoParcelaResidual.getDadosSeguroPrestamista(),
                    resultadoCalculadoParcelaResidual.getValorDoSeguroPrestamista(),
                    resultadoCalculadoParcelaResidual.getValorDoSeguroPrestamistaNaParcela(),
                        resultadoCalculadoParcelaResidual.getSeguroFranquia(),
                    valorParcelaResidual,
                    resultadoCalculadoParcelaResidual.getValorSeguroAutoNaParcela(),
                    null, null,
                    resultadoCalculadoParcelaResidual.getValorSubsidioUsado(),
                    resultadoCalculadoParcelaResidual.getTaxaSubsidioUsada(),
                    resultadoCalculadoParcelaResidual.getValorTotalPrazo(), percentualParcelaResidual,
                    resultadoCalculadoParcelaResidual.getResultadoCalculado(),
                    resultadoCalculadoParcelaResidual.getTaxaMensalCompleta(),
                    resultadoCalculadoParcelaResidual.getTaxaAnualCompleta(),
                    resultadoCalculadoParcelaResidual.getTaxaSubsidioUsadaCompleta(),
                    resultadoCalculadoParcelaResidual.getValorTaxaBanco(),
                    resultadoCalculadoParcelaResidual.getDadosCobrancaServicoGeolocalizaocao() != null ? resultadoCalculadoParcelaResidual.getDadosCobrancaServicoGeolocalizaocao() : null),
                percentualResidualMinimo,
                percentualResidualMaximo
        );
    }

    private ResultadoCalculadoParcelaResidual calcularComSeguro(SimulacaoParcDesejadaRequest request,
                                                                Map<Integer, Prazo> prazosMap,
                                                                Integer prazo,
                                                                BigDecimal valorParcelaResidual,
                                                                Integer idade, DadosCalculo dadosCalculo) {

        CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista =
                CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(
                        dadosCalculo,
                        request.getParametrosDeSeguroPrestamista(),
                        dadosCalculo.getValorBaseParaCalculoDoSeguro(),
                        null, idade, request.getTipoPessoaEnum());
        BigDecimal valorDoSeguroPrestamista = custoPercentualDoSeguroPrestamista.getFator().multiply(dadosCalculo.getValorBaseParaCalculoDoSeguro());
        SeguroFranquia seguroFranquia = new SeguroFranquia();
        seguroFranquia.setValorTotal(request.getVlrSeguroFranquia());
        seguroFranquia.setValorParcela(BigDecimal.ZERO);
        ResultadoCalculado resultadoCalculado = calcular(request, valorParcelaResidual,  prazosMap, prazo, valorDoSeguroPrestamista, dadosCalculo);
        valorParcelaResidual = new BigDecimal(resultadoCalculado.getDadosSimulacao().getParcelasBalao().get(0).getValorParcela());
        BigDecimal valorParcela = fluxoService.getValorDaParcela(resultadoCalculado);

        CustoPercentualDoSeguroPrestamista novoCustoPercentualDoSeguroPrestamista =
                CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(dadosCalculo, request.getParametrosDeSeguroPrestamista(),
                    dadosCalculo.getValorBaseParaCalculoDoSeguro(), valorParcela, idade, request.getTipoPessoaEnum());

        if (custoPercentualDoSeguroPrestamista.getTipo() != novoCustoPercentualDoSeguroPrestamista.getTipo()) {
            custoPercentualDoSeguroPrestamista = novoCustoPercentualDoSeguroPrestamista;
            valorDoSeguroPrestamista = custoPercentualDoSeguroPrestamista.getFator().multiply(dadosCalculo.getValorBaseParaCalculoDoSeguro());
            resultadoCalculado = calcular(request, valorParcelaResidual, prazosMap, prazo, valorDoSeguroPrestamista, dadosCalculo);
        }

        valorParcela = fluxoService.getValorDaParcela(resultadoCalculado);
        BigDecimal valorIOF = NumberUtils.toBigDecimal(resultadoCalculado.getValorIOF());
        BigDecimal valorCetAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaCetAnual());
        BigDecimal valorTotalFinanciado = NumberUtils.toBigDecimal(resultadoCalculado.getValorFinanciamento());
        BigDecimal valorTaxaMes = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaMes());
        BigDecimal valorTaxaAnual = NumberUtils.toBigDecimal(resultadoCalculado.getValorTaxaAnual());
        BigDecimal valorSubsidio = NumberUtils.toBigDecimal(resultadoCalculado.getValorSubsidio());
        BigDecimal valorTotalFinanciamento = NumberUtils.toBigDecimal(resultadoCalculado.getValorTotalFinanciamento());

        BigDecimal taxaBancoMotorTaxas = resultadoCalculado.getValorTaxaBanco();

        return new ResultadoCalculadoParcelaResidual(prazo,
                valorParcela, valorIOF, valorCetAnual, valorTotalFinanciado, valorTaxaMes, valorTaxaAnual, valorSubsidio,
                dadosCalculo.getSeguroGarantia(),
                custoPercentualDoSeguroPrestamista,
                valorDoSeguroPrestamista,
                BigDecimal.ZERO,
                seguroFranquia,
                null,
                BigDecimal.ZERO,
                null, null, resultadoCalculado.getValorSubsidioUsado(), resultadoCalculado.getTaxaSubsidioUsada(),
                valorTotalFinanciamento, null, resultadoCalculado,
                valorTaxaMes, valorTaxaAnual, resultadoCalculado.getTaxaSubsidioUsada(),
                taxaBancoMotorTaxas,
                null);
    }

    private BigDecimal calcularMetadeDoIntervalo(BigDecimal percentualResidualMaximo, BigDecimal percentualResidualMinimo) {
        BigDecimal percentualParcelaResidual;
        BigDecimal ajuste = percentualResidualMaximo.subtract(percentualResidualMinimo)
                .divide(new BigDecimal(2), 4, RoundingMode.HALF_UP);
        percentualParcelaResidual = percentualResidualMaximo.subtract(ajuste);
        return percentualParcelaResidual;
    }

    protected Map<Integer, Prazo> getPrazosMap(Integer planoId, BigDecimal percentualEntrada,
                                               Collection<Integer> meses, BigDecimal taxaMes) {
        return simulacaoServices.getParcelas(planoId, percentualEntrada, meses, taxaMes, null).
                stream().collect(Collectors.toMap(Prazo::getNumeroDeParcelas, p -> p));
    }

    private BigDecimal getMaxPercentualParcelaResidual(SimulacaoParcDesejadaRequest request) {
        BigDecimal percentualEntrada = request.getValorEntrada().multiply(NumberUtils.CEM)
                .divide(request.getValorBem(), 5, RoundingMode.HALF_UP);
        return NumberUtils.CEM.subtract(percentualEntrada);
    }

    /**
     * Realizar o cálculo do financiamento de um veículo
     *
     * @param valorParcelaResidual
     * @param prazosMap
     * @param prazo
     * @return
     */
    private ResultadoCalculado calcular(SimulacaoParcDesejadaRequest request,
                                          BigDecimal valorParcelaResidual, Map<Integer, Prazo> prazosMap, Integer prazo,
                                          BigDecimal valorDoSeguroPrestamista,  DadosCalculo dadosCalculo) {

        Prazo prazoObj = prazosMap.get(prazo);
        valorParcelaResidual = ajustarResidualNosLimites(prazoObj, valorParcelaResidual, request.getValorBem());

        BigDecimal valorIOFAA = dadosCalculo.getTaxasIOF().getValorIOFAA();
        BigDecimal valorIOFAD = dadosCalculo.getTaxasIOF().getValorIOFAD();
        Integer valorBaseCalculo = dadosCalculo.getTaxasIOF().getValorBaseCalculoParaIOF();

        LocalDate primeiroVencimento = request.getDataPrimeiroVencimento(dadosCalculo.getDataCalculo());
        request.setNDiasAposVencimento(false);

        List<ParcelaBalao> parcelasBalao = new ArrayList<>();
        ParcelaBalao parcelaBalao = new ParcelaBalao(
                primeiroVencimento.plusMonths(prazo - 1L),
                valorParcelaResidual
        );
        parcelasBalao.add(parcelaBalao);
        BigDecimal valorBemComSeguroAuto = request.getValorBem()
                .add(request.getValorSeguroAuto());

        DadosSimulacao simulacao = new DadosSimulacao();
        simulacao.setIdSimulacao(TipoDeSimulacao.RESIDUAL);
        simulacao.setDataSimulacao(dadosCalculo.getDataCalculo());
        simulacao.setDataPrimeiroVencimento(primeiroVencimento);
        simulacao.setValorBem(valorBemComSeguroAuto);
        simulacao.setValorEntrada(request.getValorEntrada());
        simulacao.setItens(request.getItens());
        simulacao.setValorRegistroContrato(request.getValorUfEmplacamento());
        simulacao.setValorCestaServico(request.getValorCestaServicos());
        simulacao.setValorTC(request.getValorTC());
        simulacao.setCodigoRetorno(request.getRetorno());
        simulacao.setTipoPessoa(request.getTipoPessoa());
        simulacao.setUsuarioInformouDataPrimeiroVencimento(!request.getNDiasAposVencimento());
        simulacao.setDadosSeguroMecanica(dadosCalculo.getSeguroGarantia());
        simulacao.setValorSeguroPrestamista(valorDoSeguroPrestamista);

        simulacao.setValorSubsidio(null);
        simulacao.setValorTaxaSubsidio(request.getTaxaSubsidio());

        simulacao.setPeriodicidade(request.getPeriodicidade());
        simulacao.setTipoFinanciamento(TipoFinanciamento.CDC);
        simulacao.setTipoCalculo(TipoCalculo.TAXA);
        simulacao.setTemIsencaoIOF(false);
        simulacao.setTaxaIOFAA(valorIOFAA);
        simulacao.setTaxaIOFAD(valorIOFAD);
        simulacao.setBaseCalculoIOF(valorBaseCalculo);
        simulacao.setParcelasBalao(parcelasBalao);
        simulacao.setPrazo(prazosMap.get(prazo));

        return calculadoraServices.calcular(simulacao);
    }

    private BigDecimal ajustarResidualNosLimites(Prazo prazoObj, BigDecimal valorParcelaResidual, BigDecimal valorBem) {
        valorParcelaResidual = valorParcelaResidual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal maximo = prazoObj.getPercentualMaxBalao().multiply(valorBem).divide(NumberUtils.CEM);
        BigDecimal minimo = prazoObj.getPercentualMinBalao().multiply(valorBem).divide(NumberUtils.CEM);
        if (minimo.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0) {
            // AutBank não aceita residual zero
            minimo = UM_CENTESIMO;
        }

        if (valorParcelaResidual.compareTo(maximo) > 0) {
            valorParcelaResidual = SimulacaoParcResidual.pegarMaximoArredondando(maximo);
        }
        if (valorParcelaResidual.compareTo(minimo) < 0) {
            valorParcelaResidual = SimulacaoParcResidual.pegarMinimoArredondando(minimo);
        }
        return valorParcelaResidual;
    }

    private BigDecimal calcularValorNaParcela(
            SimulacaoParcDesejadaRequest request,
            Prazo prazo, DadosCalculo dadosCalculo) {

        BigDecimal valorIOFAA = dadosCalculo.getTaxasIOF().getValorIOFAA();
        BigDecimal valorIOFAD = dadosCalculo.getTaxasIOF().getValorIOFAD();
        Integer valorBaseCalculo = dadosCalculo.getTaxasIOF().getValorBaseCalculoParaIOF();

        DadosSimulacao simulacao = new DadosSimulacao();
        simulacao.setIdSimulacao(TipoDeSimulacao.RESIDUAL);
        simulacao.setDataSimulacao(dadosCalculo.getDataCalculo());
        simulacao.setDataPrimeiroVencimento(request.getDataPrimeiroVencimento(dadosCalculo.getDataCalculo()));
        simulacao.setValorBem(DEZ_MIL);
        simulacao.setVlrSeguroFranquia(request.getVlrSeguroFranquia());
        simulacao.setValorSeguroPrestamista(BigDecimal.ZERO);
        simulacao.setCodigoRetorno(request.getRetorno());
        simulacao.setTipoPessoa(request.getTipoPessoa());
        simulacao.setValorSubsidio(null);
        simulacao.setValorTaxaSubsidio(request.getTaxaSubsidio());
        simulacao.setPeriodicidade(request.getPeriodicidade());
        simulacao.setTipoFinanciamento(TipoFinanciamento.CDC);
        simulacao.setTipoCalculo(TipoCalculo.TAXA);
        simulacao.setTemIsencaoIOF(false);
        simulacao.setTaxaIOFAA(valorIOFAA);
        simulacao.setTaxaIOFAD(valorIOFAD);
        simulacao.setBaseCalculoIOF(valorBaseCalculo);
        simulacao.setPrazo(prazo);

        return fluxoService.getValorDaParcelaComOuSemIOF(calculadoraServices.calcular(simulacao));
    }

    private void calcularEPopularValoresNaParcela(SimulacaoParcDesejadaRequest request,
                                                  List<ResultadoCalculadoParcelaDesejada> list,
                                                  Map<Integer, Prazo> prazosMap, DadosCalculo dadosCalculo) {

        for (ResultadoCalculadoParcelaDesejada r : list) {
            BigDecimal valorNaParcela = calcularValorNaParcela(request, prazosMap.get(r.getPrazo()), dadosCalculo);

            BigDecimal fator = valorNaParcela.divide(DEZ_MIL);
            SeguroFranquia seguroFranquia = r.getSeguroFranquia();
            seguroFranquia.setValorParcela(request.getVlrSeguroFranquia().multiply(fator));
            r.setValorDoSeguroPrestamistaNaParcela(r.getValorDoSeguroPrestamista().multiply(fator));
            r.setValorSeguroAutoNaParcela(request.getValorSeguroAuto().multiply(fator));
            List<ItemFinanciavel> itens = request.getItens();
            if (itens != null && !itens.isEmpty()) {
                List<ItemFinanciavel> itensFinanciaveisNaParcela = new ArrayList<>();
                itens.forEach(item -> {
                    ItemFinanciavel i = new ItemFinanciavel();
                    i.setCodigo(item.getCodigo());
                    i.setDescricao(item.getDescricao());
                    i.setValor(item.getValor());
                    i.setValorParcela(item.getValor().multiply(fator).setScale(2, RoundingMode.HALF_UP));
                    i.setEditavel(item.isEditavel());
                    i.setRemovivel(item.isRemovivel());
                    itensFinanciaveisNaParcela.add(i);
                });
                r.setItensFinanciaveisNaParcela(itensFinanciaveisNaParcela);
            }
        }
    }
}
