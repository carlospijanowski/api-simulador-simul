package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.IntermediariasResponse;
import br.com.bancotoyota.services.simulador.services.IntermediariasService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@Tag(name = "Intermediária", description = "Calculo de parcelas intermediárias para atingir o valor de parcela desejada")
public class IntermediariasController extends SimuladorExceptionController {

    @Autowired
    private IntermediariasService service;

    @Value("${simulador.intermediaria.max-porcentual}")
    private BigDecimal porcentualMaximoIntermediaria = new BigDecimal(8);

    @Value("${simulador.desejada.max-diferenca}")
    private BigDecimal toleranciaEmReais = new BigDecimal(5);

    @Operation(summary = "Retorna uma alternativa de plano ciclo-toyota",
            description = "Retorna de 1 a 3 intermediárias em dezembro para conseguir chegar ao valor desejado para a parcela mensal"
            )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(
                            mediaType = JSON_MEDIA_TYPE,
                            schema = @Schema(implementation = IntermediariasResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "    \"intermediarias\": [\n" +
                                            "        {\n" +
                                            "            \"valor\": 2631.38,\n" +
                                            "            \"vencimento\": \"20201227\"\n" +
                                            "        },\n" +
                                            "        {\n" +
                                            "            \"valor\": 2631.38,\n" +
                                            "            \"vencimento\": \"20211227\"\n" +
                                            "        },\n" +
                                            "        {\n" +
                                            "            \"valor\": 2631.38,\n" +
                                            "            \"vencimento\": \"20221227\"\n" +
                                            "        }\n" +
                                            "    ]\n" +
                                            "}"))),
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
    @PostMapping(value = {"/parcelas/desejada/intermediaria"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<IntermediariasResponse> calcularIntermediarias(@RequestBody @Valid SimulacaoParcResidualRequest request) {

        return ResponseEntity.ok(new IntermediariasResponse(service.criarIntermediarias(request,
                porcentualMaximoIntermediaria, toleranciaEmReais)));
    }
}
