package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.OfertasPrestamistaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.entities.SeguroPrestamistaPlusResponse;
import br.com.bancotoyota.services.simulador.services.DataCarencia;
import br.com.bancotoyota.services.simulador.services.SimulacaoServices;
import br.com.bancotoyota.services.simulador.services.exceptions.SeguroPrestamistaNotFoundException;
import br.com.bancotoyota.services.simulador.services.impl.prestamistaplus.SeguroPrestamistaPlusServiceImpl;
import br.com.bancotoyota.services.simulador.utils.FabricaObjetosDeUmJsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SimuladorOfertaPrestamistaControllerTest {

    private SimuladorOfertaPrestamistaController simuladorOfertaPrestamistaController;

    @Mock
    private SimuladorParcResidualController simuladorParcResidualController;
    @Mock
    private SimuladorParcDesejadaNovaController simuladorParcDesejadaNovaController;
    @Mock
    private SeguroPrestamistaPlusServiceImpl prestamistaService;
    @Mock
    private DataCarencia dataCarencia;
    @Mock
    private SimulacaoServices simulacaoService;

    private FabricaObjetosDeUmJsonUtil fabrica = new FabricaObjetosDeUmJsonUtil();

    @Before
    public void setUp() {
        this.simuladorOfertaPrestamistaController = new SimuladorOfertaPrestamistaController(prestamistaService, simuladorParcResidualController, dataCarencia,  simuladorParcDesejadaNovaController, simulacaoService);
    }

    @Test
    public void quandoRequestPorParcelaResidualEntaoRetornaOfertaPrestamistaParcelaResidual() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        when(dataCarencia.getDataCalculo())
                .thenReturn(LocalDate.of(2022, 10,26));

        OfertasPrestamistaResponse ofertasPrestamistaResponse = fabrica.criarObjetoDoJson("oferta-prestamista/simulacoes-residual-response.json", OfertasPrestamistaResponse.class);

        SimulacaoParcResidualResponse responseA = new SimulacaoParcResidualResponse();
        responseA.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(0)));
        SimulacaoParcResidualResponse responseB = new SimulacaoParcResidualResponse();
        responseB.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(1)));
        SimulacaoParcResidualResponse responseC = new SimulacaoParcResidualResponse();
        responseC.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(2)));

        when(simuladorParcResidualController.getParcelaResidual((SimulacaoParcResidualRequest) any(), any()))
                .thenReturn(ResponseEntity.ok(responseA))
                .thenReturn(ResponseEntity.ok(responseB))
                .thenReturn(ResponseEntity.ok(responseC));

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> ofertas = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaResidual(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(2, ofertas.getBody().getSimulacoes().size());
    }

    @Test(expected = SeguroPrestamistaNotFoundException.class)
    public void quandoSegurosPrestamistaByParemetrizacaoNoMetodoDeParcelaResidualRetornarNullEntaoDeveRetornarSeguroPrestamistaNotFoundException() throws IOException {
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(null);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaResidual(httpHeaders, simulacaoParcResidualRequest);
    }

    @Test
    public void quandoArrayDeSegurosEstiverVazioNoMetodoDeParcelaResidualEntaoDeveRetornarZeroSimulacoes() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        seguroPrestamistaPlusResponse.setSeguros(new ArrayList<>());
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-residual-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> ofertas = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaResidual(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(0, ofertas.getBody().getSimulacoes().size());
    }

    @Test
    public void quandoRequestPorParcelaDesejadaEntaoRetornaOfertaPrestamistaParcelaDesejada() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        when(dataCarencia.getDataCalculo())
                .thenReturn(LocalDate.of(2022, 10,26));

        OfertasPrestamistaResponse ofertasPrestamistaResponse = fabrica.criarObjetoDoJson("oferta-prestamista/simulacoes-desejada-response.json", OfertasPrestamistaResponse.class);

        SimulacaoParcResidualResponse responseA = new SimulacaoParcResidualResponse();
        responseA.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(0)));
        SimulacaoParcResidualResponse responseB = new SimulacaoParcResidualResponse();
        responseB.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(1)));
        SimulacaoParcResidualResponse responseC = new SimulacaoParcResidualResponse();
        responseC.setPrazos(Arrays.asList(ofertasPrestamistaResponse.getSimulacoes().get(2)));

        when(simuladorParcDesejadaNovaController.getParcelaDesejada((SimulacaoParcResidualRequest) any(), any()))
                .thenReturn(ResponseEntity.ok(responseA))
                .thenReturn(ResponseEntity.ok(responseB))
                .thenReturn(ResponseEntity.ok(responseC));

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-desejada-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> response = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaDesejada(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(3, response.getBody().getSimulacoes().size());
    }

    @Test(expected = SeguroPrestamistaNotFoundException.class)
    public void quandoSegurosPrestamistaByParemetrizacaoNoMetodoDeParcelaDesejadaRetornarNullEntaoDeveRetornarSeguroPrestamistaNotFoundException() throws IOException {
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(null);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-desejada-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaDesejada( httpHeaders, simulacaoParcResidualRequest);
    }

    @Test
    public void quandoArrayDeSegurosEstiverVazioNoMetodoDeParcelaDesejadaEntaoDeveRetornarZeroSimulacoes() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        seguroPrestamistaPlusResponse.setSeguros(new ArrayList<>());
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-desejada-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> ofertas = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaDesejada(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(0, ofertas.getBody().getSimulacoes().size());
    }

    @Test
    public void quandoSimulacaoResidualRetonarVazioEntaoNaoDeveAdicionarNoResponse() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        SimulacaoParcResidualResponse responseA = new SimulacaoParcResidualResponse();
        SimulacaoParcResidualResponse responseB = new SimulacaoParcResidualResponse();
        SimulacaoParcResidualResponse responseC = new SimulacaoParcResidualResponse();

        when(simuladorParcResidualController.getParcelaResidual((SimulacaoParcResidualRequest) any(), any()))
                .thenReturn(ResponseEntity.ok(responseA))
                .thenReturn(ResponseEntity.ok(responseB))
                .thenReturn(ResponseEntity.ok(responseC));

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-desejada-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> ofertas = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaResidual(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(0, ofertas.getBody().getSimulacoes().size());
    }

    @Test
    public void quandoSimulacaoDesejadaRetonarVazioEntaoNaoDeveAdicionarNoResponse() throws IOException {
        SeguroPrestamistaPlusResponse seguroPrestamistaPlusResponse = fabrica.criarObjetoDoJson("oferta-prestamista/combo-completo-response.json", SeguroPrestamistaPlusResponse.class);
        when(prestamistaService.getSegurosPrestamistaByParametrizacao(any(), any()))
                .thenReturn(seguroPrestamistaPlusResponse);

        SimulacaoParcResidualResponse responseA = new SimulacaoParcResidualResponse();
        SimulacaoParcResidualResponse responseB = new SimulacaoParcResidualResponse();
        SimulacaoParcResidualResponse responseC = new SimulacaoParcResidualResponse();

        when(simuladorParcDesejadaNovaController.getParcelaDesejada((SimulacaoParcResidualRequest) any(), any()))
                .thenReturn(ResponseEntity.ok(responseA))
                .thenReturn(ResponseEntity.ok(responseB))
                .thenReturn(ResponseEntity.ok(responseC));

        SimulacaoParcResidualRequest simulacaoParcResidualRequest = fabrica.criarObjetoDoJson("oferta-prestamista/oferta-prestamista-parcela-desejada-request.json", SimulacaoParcResidualRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("content-type", "application/json");
        httpHeaders.add("token", "sadgFASFGH354");
        httpHeaders.add("cnpj-origem-negocio", "91919940439101");

        ResponseEntity<OfertasPrestamistaResponse> ofertas = simuladorOfertaPrestamistaController.ofertaPrestamistaParcelaDesejada(httpHeaders, simulacaoParcResidualRequest);
        assertEquals(0, ofertas.getBody().getSimulacoes().size());
    }
}
