package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.*;
import br.com.bancotoyota.services.simulador.beans.request.DiferencaNaParcelaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcDesejadaRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualSimplesRequest;
import br.com.bancotoyota.services.simulador.beans.response.PrazoResidualResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcDesejadaResponse;
import br.com.bancotoyota.services.simulador.beans.response.SimulacaoParcResidualResponse;
import br.com.bancotoyota.services.simulador.controller.SimulacaoParcResidual;
import br.com.bancotoyota.services.simulador.controller.SimuladorExceptionController;
import br.com.bancotoyota.services.simulador.controller.SimuladorParcResidualController;
import br.com.bancotoyota.services.simulador.controller.completo.MocksParaTestesCompletos;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ResidualServiceTest extends MocksParaTestesCompletos {

    private ResidualService residualService;

    @Before
    public void setUp() throws IOException {
        super.setUp();
        prepararMockDeSimulacaoServices(Arrays.asList(12, 24, 36, 40, 42, 60));
        residualService = new ResidualService(controller);
    }

    /**
     * Exemplo de caso em que o valor da parcela não é atingido mas poderia ser.
     */
    @Test
    @Ignore
    public void basico_request() throws Exception {
        String json = lerArquivo("basico-request.json","desejada/");
        SimulacaoParcResidualSimplesRequest request = mapper.readValue(json, SimulacaoParcResidualSimplesRequest.class);
        request.getParametrosDeSeguroPrestamista().setSpfDisponivel(false);
        request.getParametrosDeSeguroPrestamista().setSvpDisponivel(false);


        BigDecimal limiteSuperior = new BigDecimal("23231.13");
        BigDecimal limiteInferior = new BigDecimal("23231.06");

        Integer[] p = request.getParcelas();
        request.setParcelas(new Integer[] {12});
        request.setValorParcelaResidual(limiteSuperior);
        PrazoResidualResponse prr = controller.getParcelaResidual(request).getBody().getPrazos().get(0);

        assertTrue(request.getValorParcelaDesejada().intValue() - prr.getValorParcela().intValue() == 0);

        request.setValorParcelaResidual(limiteInferior);
        prr = controller.getParcelaResidual(request).getBody().getPrazos().get(0);
        assertTrue(request.getValorParcelaDesejada().compareTo(prr.getValorParcela()) == 0);

        request.setParcelas(p);
        ResultadoParcelaResidual responseNova = residualService.parcelaDesejada(request, false);

        assertTrue(limiteInferior.compareTo(responseNova.getList().get(0).getValorParcelaResidual()) < 0);
        assertTrue(limiteSuperior.compareTo(responseNova.getList().get(0).getValorParcelaResidual()) > 0);

        assertTrue(request.getValorParcelaDesejada().compareTo(responseNova.getList().get(0).getValorParcela().setScale(2, RoundingMode.HALF_UP)) == 0);
    }

    @Test
    public void parcelaDesejada2501() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(2501));
        try {
            residualService.parcelaDesejada(request, false);
        } catch (Exception ex) {
            fail("erro inesperado: " + ex.getMessage());
        }
    }

    @Test
    @Ignore
    public void parcelaDesejada7000() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(5000));
        try {
            ResultadoParcelaResidual r = residualService.parcelaDesejada(request, false);
            long numeroDeSVPs = r.getList().stream().filter(rr -> rr.getDadosSeguroPrestamista().getTipo() == TipoDeSeguroPrestamista.SVP).count();
            // validação do caso em que não dá pra atingir o valor nem com SPF nem com SVP e o valor com SPF acaba
            // sendo menor do que com SVP por conta do limite do valor da parcela para usar esse tipo de seguro.
            assertEquals(5, numeroDeSVPs);
        } catch (Exception ex) {
            fail("erro inesperado: " + ex.getMessage());
        }
    }

    @Test
    public void parcelaDesejada7000ResidualZero() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(7000));
        request.setPermiteBalao(true);
        request.setPlanoId(2813);
        try {
            ResultadoParcelaResidual r = residualService.parcelaDesejada(request, false);
            r.getList().forEach(p -> assertEquals(SimuladorExceptionController.UM_CENTESIMO, p.getValorParcelaResidual()));
        } catch (Exception ex) {
            fail("erro inesperado: " + ex.getMessage());
        }
    }

    @Test
    public void parcelaDesejada2500() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(2500));
        try {
            residualService.parcelaDesejada(request, false);
        } catch (Exception ex) {
            fail("erro inesperado: " + ex.getMessage());
        }
    }

    @Test
    public void parcelaDesejadaResidualZero() throws Exception {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(7000));
        request.setPlanoId(2813);

        String json = mapper.writeValueAsString(request);

        SimulacaoParcDesejadaRequest drequest = mapper.readValue(json, SimulacaoParcDesejadaRequest.class);
        SimulacaoParcDesejadaResponse response = desejadaController.getParcelaDesejadaImpl(drequest);
        response.getPrazos().forEach(r -> assertEquals(SimuladorExceptionController.UM_CENTESIMO, r.getValorParcelaResidual()));
    }

    @Test
    public void parcelaDesejada() throws Exception {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setValorParcelaDesejada(new BigDecimal(2400));
        ResultadoParcelaResidual responseNova = residualService.parcelaDesejada(request, false);
        responseNova.getList().forEach(r -> System.out.println(r));

        String json = mapper.writeValueAsString(request);

        SimulacaoParcDesejadaRequest drequest = mapper.readValue(json, SimulacaoParcDesejadaRequest.class);
        SimulacaoParcDesejadaResponse response = desejadaController.getParcelaDesejadaImpl(drequest);
        response.getPrazos().forEach(r -> System.out.println(r));

        //if (true) return;

        for (int i = 0; i < 3; i++) {
            residualService.parcelaDesejada(request, false);
            desejadaController.getParcelaDesejadaImpl(drequest);
        }

        StopWatch sw = new StopWatch();
        sw.start("desejada");
        for (int i = 0; i < 20; i++) {
            desejadaController.getParcelaDesejadaImpl(drequest);
        }
        sw.stop();
        sw.start("nova desejada");
        for (int i = 0; i < 20; i++) {
            residualService.parcelaDesejada(request, false);
        }
        sw.stop();
        System.out.println(sw.prettyPrint());
    }

    private DiferencaNaParcelaRequest criarRequest() {
        DiferencaNaParcelaRequest request = new DiferencaNaParcelaRequest();
        request.setParametrosDeSeguroPrestamista(
                new ParametrosDeSeguroPrestamista(false, false, true,
                        true, true, BigDecimal.ZERO, BigDecimal.ZERO,true,true, "", Arrays.asList(),null));
        request.setTipoPessoa("pessoa-fisica");
        request.setValorUfEmplacamento(new BigDecimal("278.02"));
        request.setValorTC(new BigDecimal(550));
        request.setValorCestaServicos(new BigDecimal(500));
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setRetorno("1");
        request.setDataPrimeiroVencimento(LocalDate.of(2019, 2, 16));
        request.setPlanoId(2806);
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setValorBem(new BigDecimal(100000));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorParcelaResidual(new BigDecimal(25000));
        request.setParcelas(null);
        return request;
    }

    @Test
    public void quandoCalcularParcelasBalaoParaCicloComValorMaximoContratoExtrapoladoEntaoRetornaPrestamistaSVP() throws IOException {
        String jsonTipoA = lerArquivo("simulacao-tipo-A-tipo-de-subsidio-iguais.json","desejada/");
        String jsonTipoB = lerArquivo("simulacao-tipo-B-tipo-de-subsidio-iguais.json","desejada/");
        String jsonResultadoFinal = lerArquivo("simulacao-final-tipo-de-subsidio-iguais.json","desejada/");

        ResultadoParcelaResidual responseTipoA = mapper.readValue(jsonTipoA, ResultadoParcelaResidual.class);
        ResultadoParcelaResidual responseTipoB = mapper.readValue(jsonTipoB, ResultadoParcelaResidual.class);
        ResultadoParcelaResidual responseFinal = mapper.readValue(jsonResultadoFinal, ResultadoParcelaResidual.class);

        List<Prazo> prazos = Arrays.asList(new Prazo(48, new BigDecimal("1.54"), new BigDecimal("20"), new BigDecimal("50"), new BigDecimal(20), new BigDecimal(29.999999),null));

        PrazoResidualResponse prazoResidualResponse = new PrazoResidualResponse();
        prazoResidualResponse.setValorParcela(new BigDecimal("9607.81"));
        prazoResidualResponse.setTipoDeSeguroPrestamista(TipoDeSeguroPrestamista.SVP);
        SimulacaoParcResidualResponse residualResponse = new SimulacaoParcResidualResponse();
        residualResponse.setPrazos(Arrays.asList(prazoResidualResponse));
        ResponseEntity<SimulacaoParcResidualResponse> responseEntity = ResponseEntity.ok(residualResponse);

        SimulacaoParcResidual simulacaoParcResidual = mock(SimulacaoParcResidual.class);
        Seguro seguro = createSFPToTest();
        SimuladorParcResidualController controller = mock(SimuladorParcResidualController.class);
        this.residualService = new ResidualService(controller);

        when(controller.carregarPrazos(any()))
                .thenReturn(prazos);

        when(simulacaoParcResidual.getSPF())
                .thenReturn(seguro);

        when(controller.criarSimulacao(any(), anyBoolean(), anyList(), anyBoolean(), any(),anyBoolean()))
                .thenReturn(simulacaoParcResidual);

        when(controller.executarSimulacao(anyBoolean(), anyList(), any(), any()))
                .thenReturn(responseTipoB)
                .thenReturn(responseTipoA)
                .thenReturn(responseFinal);

        when(simulacaoParcResidual.fazerCalculoSimples())
                .thenReturn(Arrays.asList(new BigDecimal("8148.48268")));

        when(controller.getParcelaResidual(any(), (SimulacaoParcResidualRequest) any()))
                .thenReturn(responseEntity);

        when(simulacaoParcResidual.getLimitesSubsidioValor())
                .thenReturn(new LimitesSubsidioValor(null, null));

        String json = lerArquivo("request-erro-subsidio-iguais.json","desejada/");
        SimulacaoParcResidualSimplesRequest request = mapper.readValue(json, SimulacaoParcResidualSimplesRequest.class);
        ResultadoParcelaResidual resultadoParcelaResidual = residualService.parcelaDesejada(request, true);
        ResultadoCalculadoParcelaResidual resultadoCalculadoParcelaResidual = resultadoParcelaResidual.getList().get(0);

        assertEquals(new BigDecimal("9607.81"), resultadoCalculadoParcelaResidual.getValorParcela().setScale(2, RoundingMode.HALF_UP));
        assertEquals(TipoDeSeguroPrestamista.SVP, resultadoCalculadoParcelaResidual.getDadosSeguroPrestamista().getTipo());
    }

    private Seguro createSFPToTest() {
        Seguro seguro = new Seguro();
        seguro.setTipo(TipoDeSeguroPrestamista.SPF);
        seguro.setMaximoDaParcela(new BigDecimal("3500"));
        seguro.setMaximoDoContrato(new BigDecimal("200000"));
        seguro.setMaximoPorCliente(new BigDecimal("600000"));
        seguro.setPercentualDoSeguro(new BigDecimal("0.029"));
        seguro.setIdadeMinima(18);
        seguro.setIdadeMaxima(65);
        seguro.setDescricao("SEGURO PROTEÇÃO FINANCEIRA");
        seguro.setFatorDesemprego(0.0104);
        seguro.setFatorInvalidez(5.0E-4);
        seguro.setFatorMorte(0.0181);

        seguro.setFormaCalculoDescricao("Sobre Valor Financiado");
        seguro.setFormaCalculoValor("VLR_FINANCIADO");
        seguro.setItemId(1);
        seguro.setOrdenacao(1);
        seguro.setPercentualIofSpf(0.38);
        seguro.setPercentualProLabore(31.0);
        seguro.setPrazoFinal(60);
        seguro.setPrazoInicio(1);
        seguro.setTempoMaximoContrato(0);

        return seguro;
    }
}