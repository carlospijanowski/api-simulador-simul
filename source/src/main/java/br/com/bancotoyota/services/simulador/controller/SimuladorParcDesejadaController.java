package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static br.com.bancotoyota.services.simulador.controller.SimuladorExceptionController.JSON_MEDIA_TYPE;

@RestController
@Api(tags = "Financiamentos")
@ConditionalOnProperty(value="simulacao-desejada-nova.enabled", havingValue = "false", matchIfMissing = true)
public class SimuladorParcDesejadaController  {

    @Autowired
    @Getter
    private SimuladorParcDesejadaService service;

    CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices;

    private CalculadoraSeguroFranquiaServices seguroFranquiaServices;
    /**
     * Para testes apenas
     */
    public SimuladorParcDesejadaController(CalculadoraServices calculadoraService,
               SimulacaoServices simulacaoService, DataCarencia dataCarencia,
               ParametrosDeSeguroPrestamistaService parametrosDeSeguroPrestamistaService,
               @Value("${simulador.debug.retornar-request}") boolean debugRetornarRequest,SeguroPrestamistaPlusServiceImpl prestamistaService, CalculadoraSeguroFranquiaServices seguroFranquiaServices, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        service = new SimuladorParcDesejadaService(calculadoraService, simulacaoService, dataCarencia,
                parametrosDeSeguroPrestamistaService, debugRetornarRequest,prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);

    }

    /**
     * Método usado apenas em testes onde não precisamos passar o parâmetro headers.
     */
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(SimulacaoParcDesejadaRequest request) {
        return service.getParcelaDesejada(request);
    }

    public SimulacaoParcDesejadaResponse getParcelaDesejadaImpl(SimulacaoParcDesejadaRequest request) {
        return service.getParcelaDesejadaImpl(request);
    }

    /**
     * Metodo para simulação de parcela desejada. Funciona apenas para planos ciclo-toyota.
     *
     * @param request
     * @return Lista com os 10 melhores resultados dos numeros de parcela,
     * valores e parcela residual com a porcentagem
     * @throws Exception
     */
    @ApiOperation(value = "Simulação da Parcela Desejada", nickname = "parcelasDesejadaSimulacaoPost",
            notes = "Simulação da Parcela Desejada",
            response = SimulacaoParcDesejadaResponse.class, tags={ "Financiamentos", })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = SimulacaoParcDesejadaResponse.class) })
    @PostMapping(value = {"/parcelas/desejada/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(
            @ApiParam("Simulação da Parcela Desejada") @RequestBody @Valid SimulacaoParcDesejadaRequest request,
            @RequestHeader HttpHeaders headers) {

        return service.getParcelaDesejada(request, headers);
    }

    /**
     * Esse request é apenas interno, não sendo necessário atualizar o Swagger. É chamado pelo serviço de
     * propostas para a orquestração: carregar os dados para abrir na tela.
     */
    @Hidden
    @PostMapping(value = {"/parcelas/desejada/simulacao-interna"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<SimulacaoParcDesejadaResponse> getParcelaDesejada(
            @RequestHeader("token") String token,
            @RequestBody @Valid SimulacaoParcDesejadaRequest request) {
        return service.getParcelaDesejada(token, request);
    }
}
