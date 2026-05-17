package br.com.bancotoyota.services.simulador.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.response.PlanoMotorTaxa;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.*;
import br.com.bancotoyota.services.simulador.services.*;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.Setter;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroMecanicaServices;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoTaxaRequest;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoTaxaResponse;
import br.com.bancotoyota.services.simulador.entities.Subsidio.FaixaValor;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@RestController
@Validated
@Slf4j
@Tag(name = "Taxas", description = "Calculo do financiamento utilizando os valores de taxa de simulação")
public class SimuladorTaxasController extends SimuladorExceptionController {
    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private PlanoService planoService;

    @Autowired(required = false)
    @Setter(AccessLevel.PROTECTED)
    private MotorTaxaPlanoService motorTaxaPlanoService;

    private Plano plano;
    @Autowired
    private FeatureToggleConfig featureToggleConfig;

    public SimuladorTaxasController(CalculadoraServices calculadoraServices, SimulacaoServices simulacaoServices,
                                    @Value("${simulador.debug.retornar-request}") boolean debugRetornarRequest, SeguroPrestamistaPlusServiceImpl prestamistaService, CalculadoraSeguroFranquiaServices seguroFranquiaServices, CalculadoraSeguroMecanicaServices calculadoraSeguroMecanicaServices) {
        super(calculadoraServices, simulacaoServices, null, null,
                debugRetornarRequest, prestamistaService, seguroFranquiaServices, calculadoraSeguroMecanicaServices);
    }

    @Operation(summary = "Simulação de Financiamento por Taxas"
    )
    @ApiResponses(value = {

            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SimulacaoTaxaResponse.class))),

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
    @PostMapping(value = {"/parcelas/taxas/simulacao"}, consumes = JSON_MEDIA_TYPE, produces = JSON_MEDIA_TYPE)
    public ResponseEntity<SimulacaoTaxaResponse> getParcelaTaxas(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Simulação de Financiamento por Taxas", content = @Content(schema = @Schema(implementation = SimulacaoTaxaRequest.class)))
            @RequestBody @Valid SimulacaoTaxaRequest request,
            @RequestHeader HttpHeaders headers) {

        return ResponseEntity.ok(executarSimulacaoResidualTaxas(request));

    }

    private SimulacaoTaxaResponse executarSimulacaoResidualTaxas(SimulacaoTaxaRequest request) {
        log.info("executarSimulacaoResidualTaxas - conteúdo da request ao entrar no método: " + request);

        //Entrada: DadosSimulacao DadosSimulacaoPlanilha
        //Saida: ResultadoCalculado


        //Quando a origem não é enviada, ignoramos o código retorno enviado e colocamos 0 (Solução chamada da Autbank)
        if (StringUtils.isBlank(request.getOrigemSolicitacao())) {
            request.setCodigoRetorno("0");
        }

        //Monta a estrutura para executar a calculadora
        SimulacaoParcResidualRequest residualRequest = new SimulacaoParcResidualRequest(request);

        List<Prazo> prazos = new ArrayList<>();
        prazos.add(
                new Prazo(request.getPrazo(),
                        request.getTaxaBanco(),
                        BigDecimal.ZERO,
                        NumberUtils.CEM,
                        BigDecimal.ZERO,
                        NumberUtils.CEM,
                        null
                ));

        SimulacaoParcResidual r = criarSimulacaoTaxas(residualRequest, prazos, residualRequest.getDataCalculo(), request);

        ResultadoParcelaResidual result = executarSimulacao(false, prazos, null, r);

        return new SimulacaoTaxaResponse(result.getList().get(0), result.getDadosDoSubsidio(), request.calcularValorIofAdicional());
    }

    public SimulacaoParcResidual criarSimulacaoTaxas(SimulacaoParcResidualRequest residualRequest,
                                                     List<Prazo> prazos, LocalDate dataCalculo, SimulacaoTaxaRequest request) {

        BigDecimal valorFinanciado = residualRequest.calcularValorBaseParaCalculoDoSeguro();

        //Foi definido um valor fixo do valor base para 365
        IOF iof = new IOF(request.getAliquitaIof().divide(NumberUtils.CEM), request.getAliquitaIofAdicional().divide(NumberUtils.CEM), 365);

		DadosCalculo dadosCalculo = new DadosCalculo(getDataCalculo(residualRequest), iof,
				null,null, valorFinanciado, null, request.getValorSeguroPrestamista(), request.getValorSeguroGarantia());

        LimitesSubsidioValor limitesSubsidioValor = new LimitesSubsidioValor(null, null);
        if ((residualRequest.getTaxaSubsidio() != null && residualRequest.getTaxaSubsidio().compareTo(BigDecimal.ZERO) > 0)
                || (residualRequest.getValorSubsidio() != null && residualRequest.getValorSubsidio().compareTo(BigDecimal.ZERO) > 0)) {

            //Dados de Limite de Subsidio Fixos
            List<FaixaValor> faixasValor = new ArrayList<>();
            faixasValor.add(new FaixaValor(0, EnumResponsabilidade.DEALER, BigDecimal.valueOf(1), BigDecimal.valueOf(Long.parseLong("999999999999999"))));

            Subsidio subsidio = new Subsidio(EnumTipoSubsidio.VARIAVEL, null, null,
                    null, EnumTipoDistribuicao.FAIXA_VALOR, null
                    , null, faixasValor);
            Map<Integer, BigDecimal> limitePorPrazo = new HashMap<>();

            limitePorPrazo.put(request.getPrazo(), request.getValorBem());

            limitesSubsidioValor = new LimitesSubsidioValor(limitePorPrazo, subsidio);
        }

        return criarSimulacao(residualRequest, prazos, limitesSubsidioValor, valorFinanciado, dadosCalculo);
    }

    private SimulacaoParcResidual criarSimulacao(SimulacaoParcResidualRequest request, List<Prazo> prazos,
                                                 LimitesSubsidioValor limitesSubsidioValor, BigDecimal valorFinanciado, DadosCalculo dadosCalculo) {
        SimulacaoParcResidual simulacao = new SimulacaoParcResidual(calculadoraServices, limitesSubsidioValor, featureToggleConfig, seguroFranquiaServices, calculadoraSeguroMecanicaServices);

        carregarDadosDoPlano(request);

        if (nonNull(this.plano)) {
            dadosCalculo.setPlano(this.plano);
        }

        simulacao.init(request, dadosCalculo, valorFinanciado, prazos);
        return simulacao;
    }

    public ResultadoParcelaResidual executarSimulacao(boolean somenteFluxo, List<Prazo> prazos,
                                                      TipoDeSeguroPrestamista tipoFixo, SimulacaoParcResidual simulacao) {
        simulacao.limparSimulacao(prazos, tipoFixo);
        return new ResultadoParcelaResidual(simulacao.fazerCalculoTaxas(somenteFluxo),
                simulacao.getLimitesSubsidioValor().getDadosDoSubsidio());
    }

    private void carregarDadosDoPlano(SimulacaoParcResidualRequest request) {
        if (nonNull(planoService) && nonNull(request.getPlanoId())) {
            this.plano = getPlanoByToggle(request.getPlanoId());
        }
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
}
