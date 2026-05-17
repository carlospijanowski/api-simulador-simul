package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.ErrorResponse;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.entities.ClienteProspect;
import br.com.bancotoyota.services.simulador.entities.ConfiguracaoPlano;
import br.com.bancotoyota.services.simulador.entities.Entrada;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.ModalidadeSimplificada;
import br.com.bancotoyota.services.simulador.entities.ModeloVeiculos;
import br.com.bancotoyota.services.simulador.entities.SimulacaoSimplificada;
import br.com.bancotoyota.services.simulador.entities.SituacaoVeiculo;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.entities.Veiculo;
import br.com.bancotoyota.services.simulador.services.ConvertSimplificadaService;
import br.com.bancotoyota.services.simulador.services.LogService;
import br.com.bancotoyota.services.simulador.services.PropostasFinanciamentoService;
import br.com.bancotoyota.services.simulador.services.RestaUmService;
import br.com.bancotoyota.services.simulador.services.SimulacaoSimplificadaService;
import br.com.bancotoyota.services.simulador.services.VeiculosService;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import br.com.bancotoyota.services.simulador.services.impl.ConvertSimplificadaServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@Tag(name = "Simplificada", description = "Calculo de financiamento utilizando a busca automática de plano")
public class SimuladorSimplificadoController extends SimuladorExceptionController {

    @Setter
    @Autowired(required = false)
    private SimuladorParcDesejadaService simuladorParcDesejadaService;

    @Setter
    @Autowired(required = false)
    private SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController;

    private SimuladorParcResidualController simuladorParcResidualController;
    private RestaUmService restaUmService;
    private VeiculosService veiculosService;

    @Autowired(required = false)
    private ConvertSimplificadaService convertService;

    @Autowired(required = false)
    private SimulacaoSimplificadaService simulacaoSimplificadaService;

    @Autowired(required = false)
    private PropostasFinanciamentoService propostasFinanciamentoService;


    @Value("${api.veiculos.enabled:false}")
    private Boolean veiculosEnabled;

    @Value("${simulacao-simplificada-parceiros.enabled:false}")
    private Boolean simulacaoSimplificadaParceirosEnabled;

    @Value("${planos.filtro-id-modalidade.enabled:false}")
    private Boolean filtroIdModalidadeEnabled;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private LogService logService;

    @Autowired
    public SimuladorSimplificadoController(SimuladorParcResidualController simuladorParcResidualController,
                                           RestaUmService restaUmService, VeiculosService veiculosService, SimulacaoSimplificadaService simulacaoSimplificadaService, PropostasFinanciamentoService propostasFinanciamentoService) {
        this.simuladorParcResidualController = simuladorParcResidualController;
        this.restaUmService = restaUmService;
        this.convertService = new ConvertSimplificadaServiceImpl();
        this.veiculosService = veiculosService;
        this.simulacaoSimplificadaService = simulacaoSimplificadaService;
        this.propostasFinanciamentoService = propostasFinanciamentoService;
    }

    public ResponseEntity<SimulacaoSimplificadaDesejadaResponse> getParcelaDesejada(@RequestBody @Valid SimulacaoSimplificadaDesejadaRequest request) {
        return getParcelaDesejada(request, null);
    }

    @Operation(summary = "Simulação Simplificada da Parcela Desejada",
            description = "Simulação Simplificada  Parcela Desejada"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = SimulacaoSimplificadaDesejadaResponse.class))),
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
    })
    @PostMapping(value = {"/parcelas/desejada/simulacao/simplificada"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<SimulacaoSimplificadaDesejadaResponse> getParcelaDesejada(
            @Parameter(description = "Simulação Simplificada da Parcela Desejada")
            @RequestBody @Valid SimulacaoSimplificadaDesejadaRequest request,
            @RequestHeader HttpHeaders headers) {

        if (logService != null) {
            logService.logData(request, "parâmetros para simplificada desejada: ");
        }

        SimulacaoSimplificada simulacaoSimplificada = new SimulacaoSimplificada();
        SimulacaoRequest simulacaoRequest = getSimulacaoRequest(request, "", request.getModalidade(), simulacaoSimplificada);
        ResponseEntity<SimulacaoParcDesejadaResponse> response = validarModalidadeSituacaoVeiculoSimplificadaDesejada(simulacaoRequest, headers, request.getValorParcelaDesejada(), request.getModalidade(), request.getOrigemSolicitacao(), request.getParcelas());
        SimulacaoSimplificadaDesejadaResponse finalResponse = new SimulacaoSimplificadaDesejadaResponse();
        BeanUtils.copyProperties(response.getBody(), finalResponse);
        finalResponse.setValorRegistroContrato(simulacaoRequest.getValorUfEmplacamento());
        finalResponse.setValorCestaServico(simulacaoRequest.getValorCestaServicos());
        finalResponse.setValorTarifaCadastro(simulacaoRequest.getValorTC());

        if (simulacaoSimplificadaParceirosEnabled) {
            simulacaoSimplificada = simulacaoSimplificadaService.simplificadaDesejadaToSimulacaoSimplificada(simulacaoSimplificada, simulacaoRequest, finalResponse, request);
            finalResponse.setCodigoSimulacao(simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada));
        }

        return ResponseEntity.ok(finalResponse);
    }

    public ResponseEntity<SimulacaoSimplificadaResidualResponse> getParcelaResidual(@RequestBody @Valid SimulacaoSimplificadaResidualRequest request) {
        return getParcelaResidual(request, null);
    }

    @Operation(summary = "Simulação Simplificada da Parcela Residual",
            description = "Simulação Simplificada da Parcela Residual"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = SimulacaoSimplificadaResidualResponse.class))),
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
            )
    })
    @PostMapping(value = {"/parcelas/residual/simulacao/simplificada"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<SimulacaoSimplificadaResidualResponse> getParcelaResidual(
            @Parameter(description = "Simulação Simplificada da Parcela Residual")
            @RequestBody @Valid SimulacaoSimplificadaResidualRequest request,
            @RequestHeader HttpHeaders headers) {

        if (logService != null) {
            logService.logData(request, "parâmetros para simplificada residual: ");
        }

        SimulacaoSimplificada simulacaoSimplificada = new SimulacaoSimplificada();
        SimulacaoRequest simulacaoRequest = getSimulacaoRequest(request, "", null, simulacaoSimplificada);

        SimulacaoParcResidualRequest finalRequest = new SimulacaoParcResidualRequest(simulacaoRequest,
                request.getValorParcelaResidual(), request.getValorSubsidio(), request.getParcelas());

        // Como alguns métodos da simul são chamados por vários lugares, e não apenas pelo Direct,
        // passamos a OrigemSolicitacao adiante, para que com ela validemos se vai passar por este ou outro fluxo.
        finalRequest.setOrigemSolicitacao(request.getOrigemSolicitacao() != null ? request.getOrigemSolicitacao() : null);

        ResponseEntity<SimulacaoParcResidualResponse> response =
                simuladorParcResidualController.getParcelaResidual(finalRequest, headers);

        SimulacaoSimplificadaResidualResponse finalResponse = new SimulacaoSimplificadaResidualResponse();
        BeanUtils.copyProperties(response.getBody(), finalResponse);
        finalResponse.setValorRegistroContrato(simulacaoRequest.getValorUfEmplacamento());
        finalResponse.setValorCestaServico(simulacaoRequest.getValorCestaServicos());
        finalResponse.setValorTarifaCadastro(simulacaoRequest.getValorTC());


        if (simulacaoSimplificadaParceirosEnabled) {
            simulacaoSimplificada = simulacaoSimplificadaService.simplificadaResidualToSimulacaoSimplificada(simulacaoSimplificada, finalRequest, finalResponse, request);
            finalResponse.setCodigoSimulacao(simulacaoSimplificadaService.insertSimulacaoSimplificada(simulacaoSimplificada));
        }

        return ResponseEntity.ok(finalResponse);
    }


    @Operation(summary = "Consulta da Simulação Simplificada", description = "Busca uma Simulação Simplificada de Parcela Desejada/Residual pelo codigo-simulacao")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = JSON_MEDIA_TYPE, schema = @Schema(implementation = SimulacaoSimplificada.class))),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
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
    @GetMapping(value = {"/simulacao/simplificada/{codigoSimulacao}"}, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<SimulacaoSimplificada> getSimulacaoSimplificada(
            @Parameter(description = "Codigo do calculo da simulação", name = "codigoSimulacao", required = true, example = "")
            @PathVariable(name = "codigoSimulacao") @NotBlank String codigoSimulacao) {
        long startTime = System.currentTimeMillis();
        try {
            return ResponseEntity.ok(simulacaoSimplificadaService.findByCodigoSimulacao(codigoSimulacao));
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("tempo para obter a simulacao simplificada " + (endTime - startTime));
        }
    }

    @Operation(summary = "Atualiza os dados de contato das simulações simplificadas do MongoDB",
            description = "Atualiza uma Simulação Simplificada de Parcela Desejada/Residual com os dados de contato através da session-id e origem enviados"
            )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Erro.class)
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
    @PutMapping(value = {"/simulacao/simplificada/dados-contato"}, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<Void> putDadosContatoSimulacaoSimplificada(
            @Parameter(description = "Dados de Contato para simulação simplificada", required = true)
            @RequestBody @Valid DadosContatoRequest dadosContato) {
        long startTime = System.currentTimeMillis();
        try {
            ClienteProspect clienteProspect = new ClienteProspect();
            clienteProspect.setCelular(dadosContato.getCelular());
            clienteProspect.setCpfCnpj(dadosContato.getCpfCnpj());
            clienteProspect.setEmail(dadosContato.getEmail());
            clienteProspect.setNome(dadosContato.getNome());
            clienteProspect.setTelefone(dadosContato.getTelefone());
            clienteProspect.setPrazoSelecionado(dadosContato.getPrazo());
            clienteProspect.setIdOfertaSalesforce(dadosContato.getIdOfertaSalesforce());
            clienteProspect.setCodigoSimulacao(dadosContato.getCodigoSimulacao());

            //Atualiza os dados da simulacao selecionada
            simulacaoSimplificadaService.updateSimulacaoSelecionadaByCodigoSimulacao(dadosContato.getOrigem(), clienteProspect);
            //Atualiza os dados de contato
            simulacaoSimplificadaService.updateDadosContatoBySessionId(dadosContato.getSessionId(), dadosContato.getOrigem(), clienteProspect);

            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("tempo para atualizar as simulacoes simplificadas " + (endTime - startTime));
        }
    }


    @Operation(summary = "Gera o texto de aviso-legal de uma simulação simplificada",
            description = "Gera o texto de aviso-legal de uma simulação simplificada gravada no MongoDB, a busca da simulação é feita pelo codigo-simulação + origem + session-id")
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
    @PostMapping(value = {"/simulacao/simplificada/aviso-legal"}, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<TextoAvisoLegalResponse> postTextoAvisoLegalSimulacaoSimplificada(
            @Parameter(description = "Dados da Simulação Simplificada para gerar o Texto de Aviso Legal", required = true)
            @RequestBody @Valid TextoAvisoLegalRequest avisoLegalRequest) {

        SimulacaoSimplificada simulacao = simulacaoSimplificadaService.findByCodigoSimulacaoSessionIdOrigemPrazo(avisoLegalRequest.getCodigoSimulacao(), avisoLegalRequest.getSessionId(), avisoLegalRequest.getOrigem(), avisoLegalRequest.getPrazo());
        TextoAvisoLegalResponse avisoLegal = new TextoAvisoLegalResponse(propostasFinanciamentoService.gerarTextoAvisoLegalSimulacaoSimplificada(simulacao, avisoLegalRequest.getPrazo()));
        return ResponseEntity.ok(avisoLegal);
    }

    /*
     * Prepara os parâmetros para chamada da simulação
     * Quando a modalidade = null, define por default a modalidade Ciclo-Toyota
     * Quando a situacaoVeiculo = null, define por default 0km
     * Quando a marcaId e modeloId não são enviados, faz a validação se o codigoFipe foi enviado e busca os dados modeloId e marcaId pela API FIPE através do codigoFipe
     */
    private SimulacaoRequest getSimulacaoRequest(SimulacaoSimplificadaRequest request, String token, ModalidadeSimplificada modalidadeSimplificada, SimulacaoSimplificada simulacaoSimplificada) {
        Modalidade modalidade = null;
        List<String> idsModalidade = new ArrayList<>();
        if (modalidadeSimplificada == null) {
            modalidade = Modalidade.CICLO_TOYOTA;
        } else if (modalidadeSimplificada.equals(ModalidadeSimplificada.CDC)) {
            modalidade = Modalidade.CDC;
        } else if (modalidadeSimplificada.equals(ModalidadeSimplificada.LEASING)) {
            modalidade = Modalidade.LEASING;
        } else if (modalidadeSimplificada.equals(ModalidadeSimplificada.CICLO_TOYOTA)) {
            modalidade = Modalidade.CICLO_TOYOTA;
        } else if (modalidadeSimplificada.equals(ModalidadeSimplificada.CREDITO_FACIL) && filtroIdModalidadeEnabled) {
            modalidade = Modalidade.CDC;
            idsModalidade.add("18");
        } else if (modalidadeSimplificada.equals(ModalidadeSimplificada.CREDITO_INTELIGENTE) && filtroIdModalidadeEnabled) {
            modalidade = Modalidade.CDC;
            idsModalidade.add("13");
        } else {
            throw new EntityNotFoundException(Modalidade.class, "modalidade");
        }

        if (request.getSituacaoVeiculo() == null) {
            request.setSituacaoVeiculo(SituacaoVeiculo.ZERO_KM);
        }
        Integer anoModelo = request.getAnoModelo();
        if (request.getAnoModelo() == null) {
            request.setAnoModelo(LocalDate.now().get(ChronoField.YEAR));
        }
        if (request.getTipoPessoa() == null) {
            request.setTipoPessoa(TipoPessoa.PESSOA_FISICA);
        }


        ConfiguracaoPlano configuracao = new ConfiguracaoPlano(modalidade, request.getTipoPessoa(),
                request.getSituacaoVeiculo(), false, false, -1);

        //Obtém os dados do veiculo através da marcaId e modeloId
        ModeloVeiculos modeloVeiculo;
        if (veiculosEnabled) {
            if (anoModelo == null || request.getSituacaoVeiculo().equals(SituacaoVeiculo.ZERO_KM)) {
                modeloVeiculo = veiculosService.getModeloZeroKm(request.getMarcaId(), request.getModeloId(), anoModelo, token);
            } else {
                modeloVeiculo = veiculosService.getModelo(request.getMarcaId(), request.getModeloId(), anoModelo, token);
            }
            simulacaoSimplificada.setEntrada(new Entrada());
            simulacaoSimplificada.getEntrada().setVeiculo(new Veiculo(modeloVeiculo.getAnoFabricacao(), modeloVeiculo.getAnoModelo(), modeloVeiculo.getCodigoMolicar(), request.getMarcaId(), modeloVeiculo.getMarca().getDescricao(), request.getModeloId(), modeloVeiculo.getDescricaoModelo(), modeloVeiculo.getCodigoMolicar(), request.getValorBem()));
        }

        return restaUmService.getSimulacaoRequest(request, configuracao, new DadosAlternativa(), null, token, simulacaoSimplificada, idsModalidade);
    }


    /*
     * Valida a Modalidade da Simulação
     * Quando é CDC, faz uma chamada da simulação Residual com valorParcelaResidual = null, pois CDC não possui parcela residual
     * Quando a modalidade é ciclo-toyota, faz a chamada da simulação Desejada, pois ela alcança o valor da parcela desejada ajustando o valor da parcela residual
     */
    private ResponseEntity<SimulacaoParcDesejadaResponse> validarModalidadeSituacaoVeiculoSimplificadaDesejada(SimulacaoRequest simulacaoRequest, HttpHeaders headers, BigDecimal valorParcelaDesejada, ModalidadeSimplificada modalidade, String origemSolicitacao, Integer... parcelas) {
        if (modalidade != null && (modalidade.equals(ModalidadeSimplificada.CDC) || modalidade.equals(ModalidadeSimplificada.CREDITO_FACIL) || modalidade.equals(ModalidadeSimplificada.CREDITO_INTELIGENTE))) {
            SimulacaoParcResidualRequest finalRequest = new SimulacaoParcResidualRequest(simulacaoRequest, parcelas);
            finalRequest.setModalidade(Modalidade.CDC);
            finalRequest.setOrigemSolicitacao(origemSolicitacao != null ? origemSolicitacao : null);
            ResponseEntity<SimulacaoParcResidualResponse> responseResidual = simuladorParcResidualController.getParcelaResidual(finalRequest, headers);
            return ResponseEntity.ok(convertService.convertResponseResidualToDesejada(responseResidual));
        } else {
            SimulacaoParcDesejadaRequest finalRequest =
                    new SimulacaoParcDesejadaRequest(simulacaoRequest, parcelas);
            finalRequest.setValorParcelaDesejada(valorParcelaDesejada);
            finalRequest.setOrigemSolicitacao(origemSolicitacao != null ? origemSolicitacao : null);

            if (simuladorParcDesejadaNovaController == null) {
                return simuladorParcDesejadaService.getParcelaDesejada(finalRequest, headers);
            } else {
                return simuladorParcDesejadaNovaController.getParcelaDesejada(finalRequest, headers);
            }
        }
    }

}
