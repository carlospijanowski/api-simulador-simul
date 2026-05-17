package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.config.*;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * @author Luis Santos Implementação do simulador com todos os prazos
 */
@RestController
@Validated
@Slf4j
@Tag(name = "Parcela Residual V2", description = "Calculo de financiamento utilizando a parcela residual")
public class SimuladorParcResidualV2Controller extends SimuladorExceptionController {

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

    @Autowired
    private CalculadoraParcelasResidual calculadoraParcelasResidual;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private ValidacaoPlanosService validacaoPlanosService;

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
    public SimuladorParcResidualV2Controller(CalculadoraServices calculadoraService, SimulacaoServices simulacaoService,
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
    @PostMapping(value = {"v2/parcelas/residual/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcResidualResponse> getParcelaResidual(
            @Parameter(description = "Simulação da Parcela Residual")
            @RequestBody @Valid SimulacaoParcResidualRequest request,
            @RequestHeader HttpHeaders headers) {

        fatorSimulacao.setFatorTaxaDetalhes(new HashSet<>());
        fatorSimulacao.setPlanoId(request.getPlanoId());
        fatorSimulacao.setCnpjOrigemNegocio(request.getCnpjOrigemNegocio());
        fatorSimulacao.setRating(request.getRating());
        fatorSimulacao.setOrigemSolicitacao(request.getOrigemSolicitacao());

        try {
            LocalDate dataCalculo = getDataCalculo(request);

            SimulacaoParcResidualResponse simulacaoParcResidualResponse = new SimulacaoParcResidualResponse(calculadoraParcelasResidual.fazerSimulacao(request, true, false, dataCalculo, true),
                    dataCalculo, request, limiteDiferencaEmReais, false);
            motorTaxaPlanoService.applyFatorRating(simulacaoParcResidualResponse);
            return ResponseEntity.ok(simulacaoParcResidualResponse);
        } finally {
            fatorSimulacao.reset();
            finalizarRequest();
        }
    }

}
