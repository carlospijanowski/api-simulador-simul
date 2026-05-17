package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.OfertasPrestamistaResponse;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.services.DataCarencia;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.exceptions.SeguroPrestamistaNotFoundException;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

@RestController
@Slf4j
@Tag(name= "Oferta Prestamista", description = "Calcula financiamento com seguro prestamista")
public class SimuladorOfertaPrestamistaController extends SimuladorExceptionController {

    private SimuladorParcResidualController simuladorParcResidualController;
    private SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController;

    @Autowired
    public SimuladorOfertaPrestamistaController(SeguroPrestamistaPlusServiceImpl prestamistaService,
                                                SimuladorParcResidualController simuladorParcResidualController,
                                                DataCarencia dataCarencia,
                                                SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController,
                                                SimulacaoServices simulacaoService) {
        this.prestamistaService = prestamistaService;
        this.simuladorParcResidualController = simuladorParcResidualController;
        this.dataCarencia = dataCarencia;
        this.simuladorParcDesejadaNovaController = simuladorParcDesejadaNovaController;
        this.simulacaoServices = simulacaoService;
    }

    @Operation(
            description = "Retorna as simulações para cada prestamista para proposta com parcela desejada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfertasPrestamistaResponse.class))),
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
    @PostMapping(value = {"/ofertas/prestamistas/parcelas/desejada"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<OfertasPrestamistaResponse> ofertaPrestamistaParcelaDesejada(@RequestHeader HttpHeaders headers,
                                                                                       @RequestBody @Valid SimulacaoParcResidualRequest request) {
        this.simulacaoServices.validaRequestSimulacao(request);

        final LocalDate dataCalculo = getDataCalculo(request);
        final SeguroPrestamistaPlusResponse segurosPrestamista = prestamistaService.getSegurosPrestamistaByParametrizacao(dataCalculo, request.getParametrosDeSeguroPrestamista());

        OfertasPrestamistaResponse ofertasPrestamistaResponse = new OfertasPrestamistaResponse();

        try {
            segurosPrestamista.getSeguros()
                    .forEach(seguro -> {
                        SimulacaoParcResidualRequest tempRequest = request;
                        tempRequest.getParametrosDeSeguroPrestamista().setIdSeguroEscolhido(seguro.getItemId());
                        Mono.just(simuladorParcDesejadaNovaController.getParcelaDesejada(tempRequest, headers))
                                .subscribe(parcelaDesejada -> {
                                    if (!isEmpty(parcelaDesejada.getBody().getPrazos())) {
                                        ofertasPrestamistaResponse.getSimulacoes().add(parcelaDesejada.getBody().getPrazos().get(0));
                                    }
                                });
                    });
        } catch (NullPointerException ex) {
            log.error("Nenhum seguro foi encontrado");
            throw new SeguroPrestamistaNotFoundException("Nenhum seguro encontrado", ex.getMessage());
        }

        return ResponseEntity
                .ok().body(ofertasPrestamistaResponse);
    }

    @Operation(
            description = "Retorna as simulações para cada prestamista para proposta com parcela residual"
           )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OfertasPrestamistaResponse.class))),
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
    @PostMapping(value = {"/ofertas/prestamistas/parcelas/residual"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<OfertasPrestamistaResponse> ofertaPrestamistaParcelaResidual(@RequestHeader HttpHeaders headers,
                                                                                       @RequestBody @Valid SimulacaoParcResidualRequest request) {

        this.simulacaoServices.validaRequestSimulacao(request);

        final LocalDate dataCalculo = getDataCalculo(request);
        final SeguroPrestamistaPlusResponse segurosPrestamista = prestamistaService.getSegurosPrestamistaByParametrizacao(dataCalculo, request.getParametrosDeSeguroPrestamista());

        OfertasPrestamistaResponse ofertasPrestamistaResponse = new OfertasPrestamistaResponse();

        try {
            segurosPrestamista.getSeguros()
                    .forEach(seguro -> {
                        SimulacaoParcResidualRequest tempRequest = request;
                        tempRequest.getParametrosDeSeguroPrestamista().setIdSeguroEscolhido(seguro.getItemId());
                        Mono.just(simuladorParcResidualController.getParcelaResidual(tempRequest, headers))
                                .subscribe(parcelaResidual -> {
                                    if (!isEmpty(parcelaResidual.getBody().getPrazos())) {
                                        List<PrazoResidualResponse> lista = ofertasPrestamistaResponse.getSimulacoes().stream().
                                                filter(seguros -> seguros.getDadosSeguroPrestamistaUtilizado().getItemId() ==
                                                        parcelaResidual.getBody().getPrazos().get(0).getDadosUltimoSeguroDisponivel().getItemId()).
                                                collect(Collectors.toList());
                                        Boolean podeIncluir = lista.size() == 0 ? true : false;
                                        if(podeIncluir){
                                            ofertasPrestamistaResponse.getSimulacoes().add(parcelaResidual.getBody().getPrazos().get(0));
                                        }

                                    }
                                });
                    });
        } catch (NullPointerException e) {
            log.error("Nenhum seguro foi encontrado");
            throw new SeguroPrestamistaNotFoundException("Nenhum seguro encontrado", e.getMessage());
        }

        return ResponseEntity.ok().body(ofertasPrestamistaResponse.removerReplicas());
    }


}
