package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.ResultadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@ConditionalOnProperty(value = "simulacao-desejada-nova.enabled", havingValue = "true")
@Tag(name = "Parcela Desejada", description = "Calculo de financiamento utilizando a parcela desejada")
public class SimuladorParcDesejadaNovaController extends SimuladorExceptionController {

    @Autowired
    @Setter
    private ResidualService residualService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private LogService logService;

    @Value("${simulador.max-requests-externos}")
    private int maxRequestsExternos;

    @Value("${simulador.desejada.max-diferenca}")
    private BigDecimal limiteDiferencaEmReais = new BigDecimal(5);

    CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    @Autowired
    @Setter
    private ObjectMapper mapper;

    @Autowired
    public SimuladorParcDesejadaNovaController(CalculadoraServices calculadoraService, SimulacaoServices simulacaoService,
                                               DataCarencia dataCarencia,
                                               @Value("${simulador.subsidio-minimo-por-valor}") BigDecimal subsidioMinimoPorValor,
                                               ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService,
                                               @Value("${simulador.debug.retornar-request}") boolean debugRetornarRequest, SeguroPrestamistaPlusServiceImpl prestamistaService, CalculadoraSeguroFranquiaServices seguroFranquiaServices, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        super(calculadoraService, simulacaoService, dataCarencia, parametrosDeSeguroPrestamistaService, debugRetornarRequest, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
    }

    /**
     * Esse request é apenas interno, não sendo necessário atualizar o Swagger. É chamado pelo serviço de
     * propostas para a orquestração: carregar os dados para abrir na tela.
     */
    @Hidden
    @PostMapping(value = {"/parcelas/desejada/simulacao-interna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcResidualResponse> getParcelaDesejada(
            @RequestHeader("token") String token,
            @RequestBody @Valid SimulacaoParcResidualRequest request) {

        carregarDadosDoProponente(token, request);
        LocalDate dataCalculo = getDataCalculo(request);
        return ResponseEntity.ok(
                new SimulacaoParcResidualResponse(
                        residualService.parcelaDesejada(request, true),
                        dataCalculo, request, limiteDiferencaEmReais, debugRetornarRequest));
    }

    @Operation(summary = "Simulação da Parcela Desejada")
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
    @PostMapping(value = {"/parcelas/desejada/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcResidualResponse> getParcelaDesejada(
            @Parameter(name = "Simulação da Parcela Desejada")
            @RequestBody @Valid SimulacaoParcResidualRequest request,
            @RequestHeader HttpHeaders headers) {

        logHeaders(headers);
        try {
            LocalDate dataCalculo = getDataCalculo(request);
            return ResponseEntity.ok(
                    new SimulacaoParcResidualResponse(
                            residualService.parcelaDesejada(request, false),
                            dataCalculo, request, limiteDiferencaEmReais, false));
        } finally {
            finalizarRequest();
        }
    }

    /**
     * usado em testes apenas para não precisar mudar todos os testes logo de começo
     */
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(SimulacaoParcDesejadaRequest request) {
        return ResponseEntity.ok(getParcelaDesejada(request, false));
    }

    private SimulacaoParcDesejadaResponse getParcelaDesejada(SimulacaoParcDesejadaRequest request, boolean corrigirMaximo) {

        try {
            String json = mapper.writeValueAsString(request);
            SimulacaoParcResidualRequest r = mapper.readValue(json, SimulacaoParcResidualRequest.class);
            ResultadoParcelaResidual response = residualService.parcelaDesejada(r, corrigirMaximo);
            LocalDate dataCalculo = getDataCalculo(request);
            SimulacaoParcResidualResponse rr = new SimulacaoParcResidualResponse(
                    response,
                    dataCalculo, r, limiteDiferencaEmReais, false);
            json = mapper.writeValueAsString(rr);
            return mapper.readValue(json, SimulacaoParcDesejadaResponse.class);
        } catch (IOException ex) {
            throw new RuntimeException("erro convertendo chamada", ex);
        }
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
     * Usado para testar termino de processamento de request depois que a aplicação recebe o sinal de parada.
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

    public SimulacaoParcDesejadaResponse getParcelaDesejadaImpl(SimulacaoParcDesejadaRequest request) {
        return getParcelaDesejada(request, true);
    }

    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(SimulacaoParcDesejadaRequest request, HttpHeaders headers) {
        logHeaders(headers);
        try {
            return getParcelaDesejada(request);
        } finally {
            finalizarRequest();
        }
    }
}
