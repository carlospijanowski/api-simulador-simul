package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.ExecutorValidadores;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.services.simulador.validadores.ValidadorBalaoCDC;
import br.com.bancotoyota.services.simulador.validadores.ValidadorBalaoCiclo;
import br.com.bancotoyota.services.simulador.validadores.ValidadorBalaoModalidade;
import br.com.bancotoyota.services.simulador.validadores.ValidadorIntermediariasCiclo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.GERADOR_OFERTA;

/**
 * @author Luis Santos Implementação do simulador com todos os prazos
 */
@RestController
@Validated
@Slf4j
@Tag(name = "Parcela Residual", description = "Calculo de financiamento utilizando a parcela residual")
public class SimuladorParcResidualController extends SimuladorExceptionController {

    public static final String VALOR_SUBSIDIO = "valor-subsidio";
    public static final String PLANO_ID = "plano-id";
    public static final String VALOR_ENTRADA = "valor-entrada";
    private BigDecimal subsidioMinimoPorValor;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private LogService logService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private PlanoService planoService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private MotorTaxaPlanoService motorTaxaPlanoService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private ValidacaoPlanosService validacaoPlanosService;

    @Value("${simulador.max-requests-externos}")
    private int maxRequestsExternos;

    @Value("${simulador.desejada.max-diferenca}")
    private BigDecimal limiteDiferencaEmReais = new BigDecimal(5);

    @Autowired
    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;
    @Autowired
    private CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;
    @Autowired
    private FeatureToggleConfig featureToggleConfig;
    private SimulacaoParcResidual simulacaoParcResidual;

    private Plano plano;
    private FatorSimulacao fatorSimulacao;

    @Autowired
    public SimuladorParcResidualController(CalculadoraServices calculadoraService, SimulacaoServices simulacaoService,
                                           DataCarencia dataCarencia,
                                           @Value("${simulador.subsidio-minimo-por-valor}") BigDecimal subsidioMinimoPorValor,
                                           ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService,
                                           @Value("${simulador.debug.retornar-request}") boolean debugRetornarRequest,
                                           SeguroPrestamistaPlusServiceImpl prestamistaService, FeatureToggleConfig featureToggleConfig,
                                           CalculadoraSeguroFranquiaServices seguroFranquiaServices,
                                           FatorSimulacao fatorSimulacao, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        super(calculadoraService, simulacaoService, dataCarencia, parametrosDeSeguroPrestamistaService,
                debugRetornarRequest, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
        this.subsidioMinimoPorValor = subsidioMinimoPorValor;
        this.featureToggleConfig = featureToggleConfig;
        this.seguroFranquiaServices = seguroFranquiaServices;
        this.calculadoraSeguroMecanicaServices = calculadoraSeguroMecanicaServices;
        this.fatorSimulacao = fatorSimulacao;
        this.simulacaoParcResidual = new SimulacaoParcResidual(featureToggleConfig, seguroFranquiaServices);
    }

    @Hidden
    @PostMapping(value = {
            "/parcelas/residual/diferenca-iterna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<DiferencaNaParcelaResponse> calcularDiferenca(
            @RequestHeader("token") String token, @RequestBody @Valid DiferencaNaParcelaRequest request) {
        carregarDadosDoProponente(token, request);
        return calcularDiferenca(request);
    }

    @Hidden
    @GetMapping(value = {
            "/planos/validacao-interna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<DiferencasMotor> validaDiferencaPlanosMotor(
            @RequestParam(name = "plano-id", required = false) Integer planoId,
            @RequestParam(name = "cnpj-origem-negocio", required = false) String cnpjOrigemNegocio,
            @RequestParam(name = "mostrar-diferencas", required = false) Boolean mostrarDiferencas,
            @RequestParam(name = "retornar-json", required = false) Boolean retornarJson) {
        return ResponseEntity.ok(new DiferencasMotor(validacaoPlanosService.validaPlanosMotorTaxas(planoId, cnpjOrigemNegocio, mostrarDiferencas, retornarJson)));
    }

    /**
     * Realiza duas simulações, sem e com o valor do item e retorna o novo valor da
     * parcela e a diferença.
     */
    @Operation(summary = "Cálculo da diferença no valor da parcela", description = "Faz o cálculo da diferença no valor da parcela devido à adição ou remoção de um elemento "
            + "(item financiável ou seguros).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = DiferencaNaParcelaResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = {"/parcelas/residual/diferenca"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<DiferencaNaParcelaResponse> calcularDiferenca(
            @Parameter(description = "Dados para realizar o cálculo da diferença na parcela")
            @RequestBody @Valid DiferencaNaParcelaRequest request) {

        LocalDate dataCalculo = getDataCalculo(request);
        List<ResultadoCalculadoParcelaResidual> list = fazerSimulacao(copyRequestOriginal(request), true, false,
                dataCalculo, false).getList();
        if (list.size() != 1) {
            throw new IllegalStateException("para o cálculo da diferença espera-se apenas um prazo");
        }

        List<DiferencaNaParcela> resultado = new ArrayList<>(request.getValoresDosItens().length);
        ResultadoCalculadoParcelaResidual resultadoSemItem = list.get(0);

        for (BigDecimal valorDoItem : request.getValoresDosItens()) {
            request.setSeguroAuto(valorDoItem);
            list = fazerSimulacao(copyRequestOriginal(request), true, false, dataCalculo, false).getList();
            ResultadoCalculadoParcelaResidual resultadoComItem = list.get(0);

            BigDecimal valorParcelaComItem = resultadoComItem.getValorSeguroAutoNaParcela();
            BigDecimal valorParcelaSemItem = resultadoSemItem.getValorSeguroAutoNaParcela();
            DiferencaNaParcela diferenca = new DiferencaNaParcela(resultadoComItem.getValorParcela(),
                    valorParcelaComItem.subtract(valorParcelaSemItem).setScale(2, RoundingMode.HALF_UP));

            resultado.add(diferenca);
        }
        return ResponseEntity.ok(new DiferencaNaParcelaResponse(resultado.toArray(new DiferencaNaParcela[0])));
    }

    /**
     * Ao fazer a simulação alguns valores são alterados e isso pode afetar o
     * próximo passa do cálculo da diferença
     */
    private DiferencaNaParcelaRequest copyRequestOriginal(DiferencaNaParcelaRequest request) {
        DiferencaNaParcelaRequest r = new DiferencaNaParcelaRequest();
        BeanUtils.copyProperties(request, r);
        return r;
    }

    public ResponseEntity<SimulacaoParcResidualResponse> getParcelaResidual(SimulacaoParcResidualRequest request) {
        return getParcelaResidual(request, null);
    }

    /**
     * Metodo para simulação de parcela residual
     *
     * @param request
     * @return Lista de prazos com o numeroDeParcelas da parcela e os valores
     * @throws Exception
     */
    @Operation(summary = "Simulação da Parcela Residual", description = "Simulação da Parcela Residual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = SimulacaoParcResidualResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = {"/parcelas/residual/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcResidualResponse> getParcelaResidual(
            @Parameter(description = "Simulação da Parcela Residual")
            @RequestBody @Valid SimulacaoParcResidualRequest request,
            @RequestHeader HttpHeaders headers) {

        logHeaders(headers);
        fatorSimulacao.setFatorTaxaDetalhes(new HashSet<>());
        fatorSimulacao.setPlanoId(request.getPlanoId());
        fatorSimulacao.setCnpjOrigemNegocio(request.getCnpjOrigemNegocio());
        fatorSimulacao.setRating(request.getRating());
        fatorSimulacao.setOrigemSolicitacao(request.getOrigemSolicitacao());

        try {
            LocalDate dataCalculo = getDataCalculo(request);

            SimulacaoParcResidualResponse simulacaoParcResidualResponse = new SimulacaoParcResidualResponse(fazerSimulacao(request, true, false, dataCalculo, true),
                    dataCalculo, request, limiteDiferencaEmReais, false);
            motorTaxaPlanoService.applyFatorRating(simulacaoParcResidualResponse);
            return ResponseEntity.ok(simulacaoParcResidualResponse);
        } finally {
            fatorSimulacao.reset();
            finalizarRequest();
        }
    }

    public SimulacaoParcResidualResponse getParcelaResidualImpl(SimulacaoParcResidualRequest request, List<Prazo> prazos) {
        try {
            LocalDate dataCalculo = getDataCalculo(request);
            return new SimulacaoParcResidualResponse(fazerSimulacao(request, dataCalculo, prazos, null),
                    dataCalculo, request, limiteDiferencaEmReais, false);
        } finally {
            finalizarRequest();
        }
    }

    @Hidden
    @PostMapping(value = {
            "/parcelas/residual/simulacao-interna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<SimulacaoParcResidualResponse> getParcelaResidual(
            @RequestHeader("token") String token, @RequestBody @Valid SimulacaoParcResidualRequest request) {
        carregarDadosDoProponente(token, request);
        LocalDate dataCalculo = getDataCalculo(request);
        SimulacaoParcResidualResponse response = new SimulacaoParcResidualResponse(
                fazerSimulacao(request, true, false, true, dataCalculo, null, null, false), dataCalculo, request,
                limiteDiferencaEmReais, debugRetornarRequest);
        if (logService != null) {
            logService.logData(response, "resposta parcela residual");
        }
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<SimulacaoFluxoResponse> getFluxoSimulacao(SimulacaoParcResidualSimplesRequest request) {
        return getFluxoSimulacao(request, null);
    }

    @Operation(summary = "Fluxo da Simulação", description = "Fluxo da Simulação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = SimulacaoFluxoResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = {"/parcelas/fluxo/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoFluxoResponse> getFluxoSimulacao(
            @Parameter(description = "Fluxo da simulação")
            @RequestBody @Valid SimulacaoParcResidualSimplesRequest request,
            @RequestHeader HttpHeaders headers) {
        logHeaders(headers);
        fatorSimulacao.setFatorTaxaDetalhes(new HashSet<>());
        fatorSimulacao.setPlanoId(request.getPlanoId());
        fatorSimulacao.setCnpjOrigemNegocio(request.getCnpjOrigemNegocio());
        fatorSimulacao.setRating(request.getRating());
        fatorSimulacao.setOrigemSolicitacao(request.getOrigemSolicitacao());
        try {
            return ResponseEntity.ok(new SimulacaoFluxoResponse(
                    fazerSimulacao(request, true, true, getDataCalculo(request), false).getList().get(0).getFluxo()));
        } finally {
            fatorSimulacao.reset();
            finalizarRequest();
        }
    }

    @Hidden
    @PostMapping(value = {
            "/parcelas/fluxo/simulacao-interna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoFluxoResponse> getFluxoSimulacao(@RequestHeader("token") String token,
                                                                    @RequestBody @Valid SimulacaoParcResidualSimplesRequest request) {
        carregarDadosDoProponente(token, request);
        return ResponseEntity.ok(new SimulacaoFluxoResponse(
                fazerSimulacao(request, true, true, true, getDataCalculo(request), null, null, false).getList().get(0)
                        .getFluxo()));
    }

    private void logHeaders(HttpHeaders headers) {
        if (logService != null) {
            logService.logHeaders(headers);
        }
        if (headers != null) {
            delay(headers);
            limitarRequestsExternos(headers.get("forwarded"), maxRequestsExternos);
        }
    }

    /**
     * Usado para testar termino de processamento de request depois que a aplicação
     * recebe o sinal de parada.
     */
    private void delay(HttpHeaders headers) {
        List<String> delay = headers.get("delay");
        if (delay != null) {
            try {
                Thread.sleep(Integer.parseInt(delay.get(0)));
            } catch (Exception e) {
                log.error("erro processando delay", e);
            }
        }
    }

    /**
     * Calcula o maior valor para o subsidio passando taxa zero como taxa de
     * subsídio. Retorna zero caso o prazo especificado não tenha dados para o
     * plano.
     *
     * @return valor máximo (não inclusivo) que pode ser usado para o prazo
     * escolhido
     */
    @Operation(summary = "Calcula o valor máximo para o subsídio dado um prazo", description = "Valor máximo do subsídio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = ValorMaximoSubsidioResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = {
            "/parcelas/residual/valor-maximo-subsidio"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<ValorMaximoSubsidioResponse> getValorMaximoDoSubsidio(
            @RequestBody @Valid SimulacaoParcResidualSimplesRequest request) {

        Subsidio subsidio = simulacaoServices.getSubsidio(request.getPlanoId());
        if (subsidio == null || subsidio.getTipoSubsidio() != EnumTipoSubsidio.VARIAVEL) {
            throw new BusinessValidationException(PLANO_ID, request.getPlanoId().toString(),
                    "plano selecionado não suporta subsídio variável");
        }
        return ResponseEntity.ok(new ValorMaximoSubsidioResponse(
                getValorMaximoDoSubsidioImpl(request, getDataCalculo(request), null).get(request.getParcelas()[0])));
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

    /**
     * Retorna o maior valor da taxa banco entre todos os prazos menos 0.01%. O
     * desconto de 0.01% é para garantir que o valor do subsídio não será zero.
     * Poderíamos fazer o cálculo para determinar o valor da taxa correspondente ao
     * valor mínimo de 10 reais, mas optou-se por simplesmente descontar 0.01% por
     * ser bem mais rápido e por não haver a necessidade de garantir o valor exato
     * de 10 reais. Se para o valor de entrada não houver dados retorna zero. O
     * front-end deverá validar o valor de entrada antes de fazer essa chamada.
     *
     * @return valor máximo (não inclusivo) que pode ser usado para o prazo
     * escolhido
     */
    @Operation(summary = "Calculo da taxa máxima de subsidio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = TaxaMaximaSubsidioResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )

    })
    @PostMapping(value = {"/parcelas/taxa-maxima-subsidio"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<TaxaMaximaSubsidioResponse> getTaxaMaximaDoSubsidio(
            @Parameter(description = "Codigo do Plano", example = "2834")
            @RequestParam(name = PLANO_ID) int planoId,
            @Parameter(description = "Percentual de entrada", example = "70")
            @RequestParam(name = "percentual-entrada") BigDecimal percentualEntrada,
            @Parameter(description = "Prazo desejado", required = false, example = "36")
            @RequestParam(name = "prazo", required = false) Integer prazo) {

        Subsidio subsidio = simulacaoServices.getSubsidio(planoId);
        if (subsidio == null || subsidio.getTipoSubsidio() != EnumTipoSubsidio.VARIAVEL) {
            throw new BusinessValidationException(PLANO_ID, Integer.toString(planoId),
                    "plano selecionado não suporta subsídio variável");
        }

        Collection<Integer> prazos = null;
        if (prazo != null) {
            prazos = new ArrayList<>();
            prazos.add(prazo);
        }
        return ResponseEntity.ok(new TaxaMaximaSubsidioResponse(
                getMaxTaxaSubsidio(simulacaoServices.getParcelas(planoId, percentualEntrada, prazos, null, null))));
    }

    @Operation(summary = "Lista de prazos para o plano", description = "Retorna a lista de prazos para um plano sem considerar o percentual de entrada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = ListaPrazos.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping(value = {"/parcelas"}, produces = JSON_MEDIA_TYPE)
    public @ResponseBody ResponseEntity<ListaPrazos> getPrazos(
            @Parameter(description = "Codigo do Plano", example = "2835")
            @RequestParam(name = PLANO_ID) int planoId,
            // vamos colocar como opcionais por enquanto pra não quebrar o front que ainda
            // não passa o valor
            @Parameter(description = "Valor da entrada", example = "3000.00")
            @RequestParam(name = "entrada", required = false) BigDecimal valorEntrada,
            @Parameter(description = "Valor do Bem", example = "10000.00")
            @RequestParam(name = "valor-bem", required = false) BigDecimal valorBem) {

        BigDecimal percentualEntrada = null;
        if (valorEntrada != null && valorBem != null) {
            percentualEntrada = valorEntrada.divide(valorBem, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        Set<Integer> set = simulacaoServices.getParcelas(planoId, percentualEntrada, null, null, null).stream()
                .map(Prazo::getNumeroDeParcelas).collect(Collectors.toSet());
        List<Integer> list = new ArrayList<>(set);
        list.sort(Comparator.naturalOrder());
        return ResponseEntity.ok(new ListaPrazos(list));
    }

    @Operation(summary = "Data de cálculo", description = "retorna a data de cálculo do BA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = ResponseDataBA.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )

    })
    @GetMapping(value = {"/data-calculo"}, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<ResponseDataBA> getDataBA() {
        log.info("iniciando request");
        long startTime = System.currentTimeMillis();
        try {
            return ResponseEntity.ok(new ResponseDataBA(new DataBAResponse(dataCarencia.getDataCalculo())));
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("tempo para obter a data de cálculo " + (endTime - startTime));
        }
    }

    @Operation(summary = "Altera a data de cálculo", description = "Altera a data de cálculo do BA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PutMapping(value = {"/data-calculo"}, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<Void> setDataBA(
            @Parameter(description = "Nova data de cálculo", required = true)
            @RequestBody DataBAResponse data) {
        dataCarencia.setDataCalculo(data.getDataBa());
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                            boolean somenteFluxo, LocalDate dataCalculo, boolean validarResidual) {
        return fazerSimulacao(request, validarSubsidio, somenteFluxo, false, dataCalculo, null, null, validarResidual);
    }

    public ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, LocalDate dataCalculo,
                                                   List<Prazo> prazos, TipoDeSeguroPrestamista tipoFixo) {
        return fazerSimulacao(request, true, false, false, dataCalculo, prazos, tipoFixo, false);
    }

    private ResultadoParcelaResidual fazerSimulacao(SimulacaoParcResidualRequest request, boolean validarSubsidio,
                                                    boolean somenteFluxo, boolean corrigirMaximo, LocalDate dataCalculo, List<Prazo> prazos,
                                                    TipoDeSeguroPrestamista tipoFixo, boolean validarResidual) {

        if (logService != null) {
            logService.logData(request, "parâmetros para parcela residual: ");
        }

        // evitando chamar getParcelas mais de uma vez por ser uma chamada remota
        if (prazos == null) {
            prazos = carregarPrazos(request);
        }

        SimulacaoParcResidual simulacao = criarSimulacao(request, validarSubsidio, prazos, corrigirMaximo, dataCalculo, validarResidual);

        return executarSimulacao(somenteFluxo, prazos, tipoFixo, simulacao);
    }

    public ResultadoParcelaResidual executarSimulacao(boolean somenteFluxo, List<Prazo> prazos,
                                                      TipoDeSeguroPrestamista tipoFixo, SimulacaoParcResidual simulacao) {
        simulacao.limparSimulacao(prazos, tipoFixo);
        return new ResultadoParcelaResidual(simulacao.fazerCalculo(somenteFluxo),
                simulacao.getLimitesSubsidioValor().getDadosDoSubsidio());
    }

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

        DadosCalculo dadosCalculo = new DadosCalculo(getDataCalculo(request), iof,
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

    @Hidden
    @PostMapping(value = "/validacao", consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<ValidacaoResponse> validar(@Valid @RequestBody ValidacaoRequest validacaoRequest,
                                                     @RequestHeader("token") String token) {

        List<Erro> list = new ArrayList<>();

        ParametrosDeSeguroPrestamista parametros = null;
        LocalDate dataNascimento = null;
        DadosPrestamista dadosPrestamista = validacaoRequest.getDadosPrestamista();
        if (dadosPrestamista != null) {
            parametros = parametrosDeSeguroPrestamistaService.getParametros(validacaoRequest.getOrigemNegocio(),
                    dadosPrestamista.getCpfProponente(), token, null, list);

            dataNascimento = parametrosDeSeguroPrestamistaService.getDataNascimento(
                    dadosPrestamista.getDataNascimento(), dadosPrestamista.getCpfProponente(), token, list);
            parametros.setValorOriginalTotalFinanciado(dadosPrestamista.getValorOriginalTotalFinanciado());

            parametros.setPermiteSPFComBaseNumeroContratos(dadosPrestamista.getPermiteSPFComBaseNumeroContratos());
            parametros.setPermiteSVPComBaseNumeroContratos(dadosPrestamista.getPermiteSVPComBaseNumeroContratos());
            parametros.setIdSeguroEscolhido(dadosPrestamista.getIdSeguroPrestamistaEscolhido());
        }

        ValidacaoResponse response = new ValidacaoResponse(list, null, parametros);

        // só validamos o seguro prestamista se tivermos o tipo e o valor e o tipo de
        // validação for validação completa, nesse caso
        // existem outros campos que se
        // tornam obrigatórios

        if (dadosPrestamista != null) {
            // pessoa fisica
            if (validacaoRequest.getTipoDeSeguroPrestamista() != null
                    && validacaoRequest.getValorSeguroPrestamista() != null
                    && dadosPrestamista.getValorTotalFinanciado() != null
                    && EnumTipoValidacao.VALIDACAO_COMPLETA == validacaoRequest.getTipoValidacao()) {
                response.setDataNascimento(validarSeguroPrestamista(validacaoRequest, parametros, dataNascimento, list));
            }
        } else {
            // pessoa jurídica
            validarSeguroPrestamista(validacaoRequest, list);
        }

        validarPlanoFinanciamento(validacaoRequest, list);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void validarPlanoFinanciamento(ValidacaoRequest validacaoRequest, List<Erro> list) {

        BigDecimal percentualEntrada = validacaoRequest.getValorEntrada().multiply(NumberUtils.CEM)
                .divide(validacaoRequest.getValorBem(), 2, RoundingMode.HALF_UP);

        List<Prazo> prazos = carregarPrazos(validacaoRequest, list, percentualEntrada);

        Plano plano = carregaPlanoParaValidacao(validacaoRequest);

        if (Objects.nonNull(prazos)) {
        	Prazo prazoComTaxaCorreta = (Objects.nonNull(validacaoRequest) && Objects.nonNull(validacaoRequest.getTaxaMensal()))
        		    ? prazos.stream()
        		        .filter(prazo -> Objects.nonNull(prazo.getTaxaBanco())
        		            && prazo.getTaxaBanco().compareTo(validacaoRequest.getTaxaMensal()) == 0)
        		        .findFirst()
        		        .orElse(null)
        		    : null;

            Prazo prazo = Objects.nonNull(prazoComTaxaCorreta) ? prazoComTaxaCorreta : prazos.iterator().next();

            if (plano != null && plano.getParcelasIntermediarias() != null && plano.getParcelasIntermediarias().getPermiteBalao()) {
                if (validacaoRequest.getModalidade() == Modalidade.CICLO_TOYOTA && validacaoRequest.getValorResidual() != null) {
                    validarResidual(list, validacaoRequest.getValorResidual(), validacaoRequest.getValorBem(), prazo);
                } else {
                    validarIntermediarias(list, validacaoRequest.getIntermediarias(), validacaoRequest.getValorBem(),
                            prazo);
                }

                validarEntradaContraIntermediarias(validacaoRequest, list);
            }

            Subsidio subsidio = simulacaoServices.getSubsidio(validacaoRequest.getCodigoPlano());
            // Para calcular o valor do subsídio seria necessário fazer uma simulação, desta
            // forma vamos validar
            // apenas a taxa
            BigDecimal txSub = validacaoRequest.getTaxaSubsidio();
            if (subsidio == null) {
                validarPlanoSemSubsidio(validacaoRequest, list, prazo);
            } else if (subsidio.getTipoSubsidio().equals(EnumTipoSubsidio.VARIAVEL)) {
                validarPlanoComSubsidio(validacaoRequest, list, prazo, subsidio, txSub);
            }
        }
    }

    private void validarEntradaContraIntermediarias(ValidacaoRequest validacaoRequest, List<Erro> list) {
        if (validacaoRequest.getIntermediarias() != null && !validacaoRequest.getIntermediarias().isEmpty()) {
            BigDecimal valorIntermediarias = validacaoRequest.getIntermediarias().stream().map(x -> x.getValor())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valorBem = validacaoRequest.getValorBem();
            BigDecimal valorEntrada = validacaoRequest.getValorEntrada();

            BigDecimal valorEntradaIntermediarias = valorEntrada.add(valorIntermediarias);
            if (valorEntradaIntermediarias.compareTo(valorBem) > 0) {
                Erro erro = new Erro();
                erro.setId(EnumErroValidacao.ERRO_VALOR_ENTRADA_INTERMEDIARIAS_MAIOR_QUE_BEM.getKey());
                erro.setMessage(EnumErroValidacao.ERRO_VALOR_ENTRADA_INTERMEDIARIAS_MAIOR_QUE_BEM.getValue());
                erro.setField(VALOR_ENTRADA);
                list.add(erro);
            }
        }
    }

    private Plano carregaPlanoParaValidacao(ValidacaoRequest validacaoRequest) {
        try {

            return getPlanoByToggle(validacaoRequest.getCodigoPlano());
            //return this.planoService.getPlanoById(validacaoRequest.getCodigoPlano());
        } catch (EntityNotFoundException ex) {
            log.error("Plano não encontrado", ex);
            return null;
        }

    }

    private void validarPlanoSemSubsidio(ValidacaoRequest validacaoRequest, List<Erro> list, Prazo prazo) {
        // Por hora, para evitar ter que fazer uma simulação, vamos validar a taxa
        // mensal apenas se o retorno for zero.
        if ("0".equals(validacaoRequest.getRetorno()) && validacaoRequest.getTaxaMensal() != null) {
            BigDecimal diferencaDeTaxa = prazo.getTaxaBanco().subtract(validacaoRequest.getTaxaMensal());
            diferencaDeTaxa = diferencaDeTaxa.setScale(2, RoundingMode.HALF_UP);
            if (diferencaDeTaxa.compareTo(BigDecimal.ZERO) != 0) {
                list.add(new Erro(null, null, EnumErroValidacao.ERRO_PLANO_SEM_SUBSIDIO_TAXA_MENSAL_INVALIDA.getKey(),
                        String.format(EnumErroValidacao.ERRO_PLANO_SEM_SUBSIDIO_TAXA_MENSAL_INVALIDA.getValue(),
                                prazo.getTaxaBanco())));
            }
        }
    }

    private List<Prazo> carregarPrazos(ValidacaoRequest validacaoRequest, List<Erro> list,
                                       BigDecimal percentualEntrada) {
        List<Integer> meses = Arrays.asList(validacaoRequest.getPrazo());
        List<Prazo> prazos = null;

        try {
            prazos = simulacaoServices.getParcelas(validacaoRequest.getCodigoPlano(), percentualEntrada, meses, null, null);
        } catch (BusinessValidationException ex) {
            // entrada fora do limite ou número de parcelas inválido
            list.add(new Erro(null, null, ex.getId(), ex.getMessage()));
        } catch (EntityNotFoundException ex) {
            list.add(new Erro(null, null, EnumErroValidacao.ERRO_PLANO_NAO_ENCONTRADO.getKey(),
                    EnumErroValidacao.ERRO_PLANO_NAO_ENCONTRADO.getValue()));
        }
        return prazos;
    }

    private boolean deveValidarBalao(Prazo prazo) {
        BigDecimal maxBalao = prazo.getPercentualMaxBalao();
        BigDecimal minBalao = prazo.getPercentualMinBalao();
        return maxBalao.compareTo(minBalao) > 0;
    }

    private void validarResidual(List<Erro> list, BigDecimal valorResidual, BigDecimal valorBem, Prazo prazo) {
        if (deveValidarBalao(prazo)) {
            BigDecimal percentualResidual = valorResidual.multiply(NumberUtils.CEM).divide(valorBem, 2,
                    RoundingMode.HALF_UP);
            if (prazo.getPercentualMaxBalao().compareTo(percentualResidual) < 0) {
                list.add(new Erro(null, null, EnumErroValidacao.ERRO_PERCENTUAL_RESIDUAL_SUPERIOR_LIMITE.getKey(),
                        String.format(EnumErroValidacao.ERRO_PERCENTUAL_RESIDUAL_SUPERIOR_LIMITE.getValue(),
                                prazo.getPercentualMaxBalao())));
            } else if (prazo.getPercentualMinBalao().compareTo(percentualResidual) > 0) {
                list.add(new Erro(null, null, EnumErroValidacao.ERRO_PERCENTUAL_RESIDUAL_INFERIOR_LIMITE.getKey(),
                        String.format(EnumErroValidacao.ERRO_PERCENTUAL_RESIDUAL_INFERIOR_LIMITE.getValue(),
                                prazo.getPercentualMinBalao())));
            }
        }
    }

    private void validarIntermediarias(List<Erro> list, List<ParcelaIntermediaria> intermediarias, BigDecimal valorBem,
                                       Prazo prazo) {
        if (intermediarias != null && deveValidarBalao(prazo)) {
            intermediarias.forEach(i -> {
                BigDecimal percentualBalao = i.getValor().multiply(NumberUtils.CEM).divide(valorBem, 2,
                        RoundingMode.HALF_UP);
                if (prazo.getPercentualMaxBalao().compareTo(percentualBalao) < 0) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE.getKey(),
                            String.format(EnumErroValidacao.ERRO_PERCENTUAL_BALAO_SUPERIOR_LIMITE.getValue(),
                                    prazo.getPercentualMaxBalao())));
                } else if (prazo.getPercentualMinBalao().compareTo(percentualBalao) > 0) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE.getKey(),
                            String.format(EnumErroValidacao.ERRO_PERCENTUAL_BALAO_INFERIOR_LIMITE.getValue(),
                                    prazo.getPercentualMinBalao())));
                }
            });
        }

    }

    private void validarPlanoComSubsidio(ValidacaoRequest validacaoRequest, List<Erro> list, Prazo prazo,
                                         Subsidio subsidio, BigDecimal txSub) {
        if (txSub == null && validacaoRequest.getValorSubsidio() == null) {
            list.add(new Erro(null, null, EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_SEM_TAXA.getKey(),
                    EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_SEM_TAXA.getValue()));
        } else {
            if (validacaoRequest.getTaxaSubsidio() != null) {
                validarTaxaSubsidio(validacaoRequest, list, prazo, subsidio, txSub);
            } else {
                validarValorSubsidio(validacaoRequest, list);
            }

        }
    }

    private void validarValorSubsidio(ValidacaoRequest validacaoRequest, List<Erro> list) {
        if (validacaoRequest.getValorMaximoSubsidio().compareTo(validacaoRequest.getValorSubsidio()) < 0
                || validacaoRequest.getValorSubsidio().compareTo(subsidioMinimoPorValor) < 0) {
            Erro erro = new Erro();
            erro.setField(VALOR_SUBSIDIO);
            erro.setValue(validacaoRequest.getValorSubsidio().toString());
            erro.setMessage("O valor do subsídio é de R$ " + NumberUtils.format(subsidioMinimoPorValor) + " a R$ "
                    + NumberUtils.format(validacaoRequest.getValorMaximoSubsidio()));
            erro.setId(EnumErroValidacao.ERRO_SUBSIDIO_FORA_FAIXA.getKey());
            list.add(erro);
        }

    }

    private void validarTaxaSubsidio(ValidacaoRequest validacaoRequest, List<Erro> list, Prazo prazo, Subsidio subsidio,
                                     BigDecimal txSub) {
        if (subsidio.getTipoSubsidio() == EnumTipoSubsidio.FIXO) {
            BigDecimal taxa = subsidio.getTaxaBanco();
            if (taxa.compareTo(prazo.getTaxaBanco()) > 0) {
                taxa = prazo.getTaxaBanco();
            }
            BigDecimal taxaInicialAceitavel = taxa.subtract(Constants.UM_CENTESIMO_PORCENTO).setScale(txSub.scale(),
                    RoundingMode.DOWN); // Tem um cenário no
            // CalculadoraServicesImpl
            // que se tira um
            // centesimo da taxa
            // , portanto , deve
            // ser contemplado a
            // possibilidade na
            // validação
            Boolean subsidioNaFaixa = txSub.compareTo(taxaInicialAceitavel) >= 0 && txSub.compareTo(taxa) <= 0;
            if (!subsidioNaFaixa) {
                list.add(new Erro(null, null, EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_INVALIDA.getKey(),
                        String.format(EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_INVALIDA.getValue(), taxa)));
            }
        } else {
            if (txSub.compareTo(BigDecimal.ZERO) < 0) {
                list.add(new Erro(null, null, EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MINIMA_ERRADA.getKey(),
                        EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MINIMA_ERRADA.getValue()));
            } else {
                if (txSub.compareTo(prazo.getTaxaBanco()) > 0) {
                    list.add(new Erro(null, null,
                            EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MAXIMA_EXCEDIDA.getKey(),
                            String.format(EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MAXIMA_EXCEDIDA.getValue(),
                                    prazo.getTaxaBanco())));
                }
            }
        }
        if (validacaoRequest.getTaxaMensal() != null && txSub.compareTo(validacaoRequest.getTaxaMensal()) != 0) {
            list.add(new Erro(null, null,
                    EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MES_IGUAL_TAXA_SUBSIDIO.getKey(),
                    EnumErroValidacao.ERRO_PLANO_COM_SUBSIDIO_TAXA_MES_IGUAL_TAXA_SUBSIDIO.getValue()));
        }
    }

    private Integer getIdade(LocalDate dataNascimento) {
        Integer idade = null;
        if (dataNascimento != null) {
            idade = dataNascimento.until(LocalDate.now()).getYears();
        }
        return idade;
    }

    /**
     * Faz a validação para o caso de pessoa jurídica
     */
    private void validarSeguroPrestamista(ValidacaoRequest validacaoRequest, List<Erro> list) {
        if (validacaoRequest.getTipoDeSeguroPrestamista() != TipoDeSeguroPrestamista.NENHUM) {
            list.add(new Erro(null, null, EnumErroValidacao.ERRO_TIPO_SEGURO_PRESTAMISTA_INVALIDO.getKey(),
                    String.format(EnumErroValidacao.ERRO_TIPO_SEGURO_PRESTAMISTA_INVALIDO.getValue(),
                            TipoDeSeguroPrestamista.NENHUM)));
        }

        if (validacaoRequest.getValorSeguroPrestamista().compareTo(BigDecimal.ZERO) != 0) {
            list.add(new Erro(null, null, EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_NENHUM.getKey(),
                    EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_NENHUM.getValue()));
        }
    }

    /**
     * Faz a validação para o caso de pessoa física
     */
    private LocalDate validarSeguroPrestamista(ValidacaoRequest validacaoRequest,
                                               ParametrosDeSeguroPrestamista parametros, LocalDate dataNascimento, List<Erro> list) {

        DadosPrestamista dadosPrestamista = validacaoRequest.getDadosPrestamista();

        // esse serviço nunca será chamado caso tipo e valor de seguro sejam null
        parametros.setSeguroPrestamistaEscolhido(dadosPrestamista.getTipoDeSeguroPrestamistaEscolhido());
        Integer idade = getIdade(dataNascimento);

        CustoPercentualDoSeguroPrestamista seguro = getCustoPercentualDoSeguroPrestamista(parametros, dadosPrestamista,
                idade);

        if (validacaoRequest.getTipoDeSeguroPrestamista() != null
                && seguro.getTipo() != validacaoRequest.getTipoDeSeguroPrestamista()) {
            list.add(new Erro(null, null, EnumErroValidacao.ERRO_TIPO_SEGURO_PRESTAMISTA_INVALIDO.getKey(), String
                    .format(EnumErroValidacao.ERRO_TIPO_SEGURO_PRESTAMISTA_INVALIDO.getValue(), seguro.getTipo())));
        }

        if (validacaoRequest.getValorSeguroPrestamista() != null) {
            BigDecimal valorSeguroPrestamista = dadosPrestamista.getValorTotalFinanciado()
                    .subtract(dadosPrestamista.getValorIof()).subtract(validacaoRequest.getValorSeguroPrestamista())
                    .multiply(seguro.getFator()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal valorSeguroPrestamistaPassado = validacaoRequest.getValorSeguroPrestamista().setScale(2,
                    RoundingMode.HALF_UP);
            log.debug("validação do seguro prestamista, esperado: " + valorSeguroPrestamista + " passado: "
                    + valorSeguroPrestamistaPassado);
            if (valorSeguroPrestamista.compareTo(valorSeguroPrestamistaPassado) != 0) {
                if (validacaoRequest.getTipoDeSeguroPrestamista() == TipoDeSeguroPrestamista.SPF) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_SPF.getKey(),
                            EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_SPF.getValue()));
                } else if (validacaoRequest.getTipoDeSeguroPrestamista() == TipoDeSeguroPrestamista.SVP) {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_SVP.getKey(),
                            EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_SVP.getValue()));
                } else {
                    list.add(new Erro(null, null, EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_NENHUM.getKey(),
                            EnumErroValidacao.ERRO_SEGURO_PRESTAMISTA_INVALIDO_NENHUM.getValue()));
                }
            }
        }
        return dataNascimento;
    }

    private CustoPercentualDoSeguroPrestamista getCustoPercentualDoSeguroPrestamista(
            ParametrosDeSeguroPrestamista parametros, DadosPrestamista dadosPrestamista, Integer idade) {
        String cnpjOrigemNegocio = null;
        List<Integer> segurosPrestamistasExcluidos = null;
        boolean removerTodosSPF = false;
        boolean removerTodosSVP = false;

        if (ObjectUtils.isNotEmpty(parametros)) {
            cnpjOrigemNegocio = parametros.getCnpjOrigemNegocio();
            segurosPrestamistasExcluidos = parametros.getSegurosPrestamistasExcluidos();
            removerTodosSPF = parametros.isSpfRemovido();
            removerTodosSVP = parametros.isSvpRemovido();
        }
        DadosCalculo dadosCalculo = new DadosCalculo(null, null,
                prestamistaService.getSegurosPrestamista(cnpjOrigemNegocio, dataCarencia.getDataCalculo(), segurosPrestamistasExcluidos, removerTodosSPF, removerTodosSVP), null, null, null, null);
        return CustoPercentualDoSeguroPrestamista.getFatorDoSeguro(dadosCalculo, parametros,
                dadosPrestamista.getValorTotalFinanciado(), dadosPrestamista.getValorParcela(), idade,
                TipoPessoa.PESSOA_FISICA);
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
            request.setDataCalculo(this.getDataCalculo(request));
        }

        if (Objects.isNull(request.getDataCalculo()) && Objects.nonNull(dataCalculo)) {
            request.setDataCalculo(dataCalculo);
        } else {
            request.setDataCalculo(this.getDataCalculo(request));
        }
    }

}
