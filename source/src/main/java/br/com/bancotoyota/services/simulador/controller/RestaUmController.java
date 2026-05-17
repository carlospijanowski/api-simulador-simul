package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.request.RestaUmRequest;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaPrazoResponse;
import br.com.bancotoyota.services.simulador.beans.response.AlternativaResponse;
import br.com.bancotoyota.services.simulador.services.RestaUmService;

import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Tag(name = "Alternativa Ciclo Toyota", description = "Retorna uma alternativa de plano ciclo toyota")
public class RestaUmController extends SimuladorExceptionController {

    private RestaUmService restaUmService;

    @Autowired
    public RestaUmController(RestaUmService restaUmService) {
        this.restaUmService = restaUmService;
    }

    @Operation(
            description = "Retorna uma alternativa de plano ciclo-toyota baseado nos parâmetros usados no CDC")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = JSON_MEDIA_TYPE,
                            schema = @Schema(implementation = AlternativaResponse.class)
                    )

            ),
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
    @PostMapping(value = {"/parcelas/desejada/alternativa"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<AlternativaResponse> opcaoCicloToyota(
            @RequestBody @Valid RestaUmRequest request,
            @Parameter(name = "perfil", description = "Perfil do consultar logado no Direct", example = "F&I")
            @RequestHeader(value = "perfil") String perfilUsuario,
            @RequestHeader(value = "token", required = false) String token) {

        return ResponseEntity.ok(restaUmService.opcaoCicloToyota(request, perfilUsuario, token));
    }

    @Hidden
    @PostMapping(value = {"/parcelas/residual/alternativa"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    @ResponseBody
    public ResponseEntity<AlternativaPrazoResponse> opcaoCicloToyotaPrazo(
            @RequestBody @Valid RestaUmRequest request,
            @RequestHeader(value = "perfil") String perfilUsuario,
            @RequestHeader(value = "token", required = false) String token) {

        return ResponseEntity.ok(restaUmService.opcaoCicloToyotaAPrazo(request, perfilUsuario, token));
    }
}
