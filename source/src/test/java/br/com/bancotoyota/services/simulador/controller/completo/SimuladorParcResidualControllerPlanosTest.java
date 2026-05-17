package br.com.bancotoyota.services.simulador.controller.completo;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.*;
import br.com.bancotoyota.services.simulador.beans.response.*;
import br.com.bancotoyota.services.simulador.controller.SimuladorExceptionController;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.OrigemNegocio;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class SimuladorParcResidualControllerPlanosTest extends MocksParaTestesCompletos {

    @Before
    public void setUp() throws IOException {
        super.setUp();
        prepararMockDeSimulacaoServices(Arrays.asList(24));
    }

    @Test
    public void testaListaDePrazos() {
        reset(motorTaxaPlanoService);
        when(motorTaxaPlanoService.findPrazos(any(), any())).then(i -> dadosDoPlano2812());
        List<Integer> prazos = controller.getPrazos(2812, null, null).getBody().getPrazos();
        assertEquals(25, prazos.size());
    }

    @Test
    public void calcularDiferenca() {

        DiferencaNaParcelaRequest request = criarRequest();

        request.setValoresDosItens(new BigDecimal[]{new BigDecimal("4000")});
        request.setParcelas(new Integer[]{24});

        // valores escolhidos de tal forma a dar SPF sem o item e SVP com o item
        DiferencaNaParcelaResponse response = controller.calcularDiferenca(request).getBody();

        // valores abaixo calculados no Excel
        BigDecimal valorComItem = new BigDecimal("201.457536");
        BigDecimal valorSemItem = BigDecimal.ZERO;
        BigDecimal diferenca = valorComItem.subtract(valorSemItem).setScale(2, RoundingMode.HALF_UP);

        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[0].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[0].getDiferencaNoValorDaParcela()));
    }

    @Test
    public void calcularDiferencaInterna() {

        DiferencaNaParcelaRequest request = criarRequest();
        request.setOrigemNegocio(new OrigemNegocio());
        request.setCpfProponente("cpf");

        request.setValoresDosItens(new BigDecimal[]{new BigDecimal("4000")});
        request.setParcelas(new Integer[]{24});

        when(parametrosDeSeguroPrestamistaService.getParametros(any(), anyString(), anyString(), any(), isNull())).thenReturn(
                new ParametrosDeSeguroPrestamista(false, false, true,
                        true, true, BigDecimal.ZERO, BigDecimal.ZERO, true, true, "", Arrays.asList(), null));

        // valores escolhidos de tal forma a dar SPF sem o item e SVP com o item
        DiferencaNaParcelaResponse response = controller.calcularDiferenca("token", request).getBody();

        // valores abaixo calculados no Excel
        BigDecimal valorComItem = new BigDecimal("201.457536");
        BigDecimal valorSemItem = BigDecimal.ZERO;
        BigDecimal diferenca = valorComItem.subtract(valorSemItem).setScale(2, RoundingMode.HALF_UP);

        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[0].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[0].getDiferencaNoValorDaParcela()));
    }

    /**
     * Esse teste usa o residual mínimo para validar que o valor do item não está sendo adicionado no valor do bem
     * uma vez que isso mudaria o percentual do residual o que geraria um erro no cálculo.
     */
    @Test
    public void calcularDiferencaNoLimiteDoResidual() {

        DiferencaNaParcelaRequest request = criarRequest();
        // a parcela residual deve ser a menor para esse teste
        request.setValorParcelaResidual(new BigDecimal("20000"));

        request.setValoresDosItens(new BigDecimal[]{new BigDecimal("4000")});
        request.setParcelas(new Integer[]{24});

        // valores escolhidos de tal forma a dar SPF sem o item e SVP com o item
        DiferencaNaParcelaResponse response = controller.calcularDiferenca(request).getBody();

        // valores abaixo calculados no Excel
        BigDecimal valorComItem = new BigDecimal("201.457536");
        BigDecimal valorSemItem = BigDecimal.ZERO;
        BigDecimal diferenca = valorComItem.subtract(valorSemItem).setScale(2, RoundingMode.HALF_UP);

        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[0].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[0].getDiferencaNoValorDaParcela()));
    }

    @Test
    public void calcularDiferencaDupla() {

        DiferencaNaParcelaRequest request = criarRequest();

        request.setValoresDosItens(new BigDecimal[]{new BigDecimal("4000"), new BigDecimal("4000")});
        request.setParcelas(new Integer[]{24});

        // valores escolhidos de tal forma a dar SPF sem o item e SVP com o item
        DiferencaNaParcelaResponse response = controller.calcularDiferenca(request).getBody();

        // valores abaixo calculados no Excel
        BigDecimal valorComItem = new BigDecimal("201.457536");
        BigDecimal valorSemItem = BigDecimal.ZERO;
        BigDecimal diferenca = valorComItem.subtract(valorSemItem).setScale(2, RoundingMode.HALF_UP);

        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[0].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[0].getDiferencaNoValorDaParcela()));
        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[1].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[1].getDiferencaNoValorDaParcela()));
    }

    @Test
    public void calcularDiferencaNegativa() {

        DiferencaNaParcelaRequest request = criarRequest();
        request.setValoresDosItens(new BigDecimal[]{new BigDecimal("-4000")});
        request.setParcelas(new Integer[]{24});

        // valores escolhidos de tal forma a dar SPF sem o item e SVP com o item
        DiferencaNaParcelaResponse response = controller.calcularDiferenca(request).getBody();

        // valores abaixo calculados no Excel
        // subtraindo 1 centavo porque agora o arredondamento é feito depois da subtração
        BigDecimal valorComItem = new BigDecimal("201.457536");
        BigDecimal valorSemItem = BigDecimal.ZERO;
        // subtraindo 1 centavo porque agora o arredondamento é feito depois da subtração
        BigDecimal diferenca = valorComItem.subtract(valorSemItem).multiply(new BigDecimal("-1"))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals("valor esperado " + diferenca + " - " + response.getDiferencas()[0].getDiferencaNoValorDaParcela(),
                0, diferenca.compareTo(response.getDiferencas()[0].getDiferencaNoValorDaParcela()));
    }

    @Test
    @Ignore
    public void calcularMaiorSubsidioPorValor() {

        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setPlanoId(request.getPlanoId() + 1); // usando plano com subsídio

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{24}));
        request.setParcelas(new Integer[]{24});

        BigDecimal response = controller.getValorMaximoDoSubsidio(request).getBody().getValorMaximoSubsidio();
        System.out.println(response);
        BigDecimal esperado = new BigDecimal("12158.94");
        assertEquals(esperado, response);

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12}));
        request.setParcelas(new Integer[]{12});

        BigDecimal response2 = controller.getValorMaximoDoSubsidio(request).getBody().getValorMaximoSubsidio();
        System.out.println(response2);
        assertEquals(new BigDecimal("6666.94"), response2);
    }

    @Test
    public void getValorMaximoDoSubsidioComPlanoSemSubsídio() {

        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setPlanoId(request.getPlanoId());

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{24}));
        request.setParcelas(new Integer[]{24});

        try {
            controller.getValorMaximoDoSubsidio(request).getBody().getValorMaximoSubsidio();
            throw new AssertionError("essa chamada deve falhar");
        } catch (BusinessValidationException ex) {
            assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
        }
    }

    @Test
    public void getValorMaximoDoSubsidioComPlanoDeSubsidioFixo() {

        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setPlanoId(request.getPlanoId() + 2);

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{24}));
        request.setParcelas(new Integer[]{24});

        try {
            controller.getValorMaximoDoSubsidio(request).getBody().getValorMaximoSubsidio();
            throw new AssertionError("essa chamada deve falhar");
        } catch (BusinessValidationException ex) {
            assertEquals("plano selecionado não suporta subsídio variável", ex.getMessage());
        }
    }

    @Test
    public void calcularMaiorSubsidioPorTaxa() {

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36}));

        TaxaMaximaSubsidioResponse r = controller.getTaxaMaximaDoSubsidio(2807, new BigDecimal("25"), null).getBody();

        BigDecimal response = r.getTaxaMaximaSubsidio();
        System.out.println(response);
        assertEquals(new BigDecimal("1.890000"), response);
    }

    @Test
    public void calcularMaiorSubsidioPorTaxaPassandoPrazo() {

        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36}));

        TaxaMaximaSubsidioResponse r = controller.getTaxaMaximaDoSubsidio(2807, new BigDecimal("25"), 12).getBody();

        BigDecimal response = r.getTaxaMaximaSubsidio();
        System.out.println(response);
        assertEquals(new BigDecimal("1.590000"), response);
    }

    @Test(expected = BusinessValidationException.class)
    public void calcularMaiorSubsidioPorTaxaForaDasFaixas() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36}));
        // não existe faixa de entrada para 31%
        controller.getTaxaMaximaDoSubsidio(2806, new BigDecimal("31"), null).getBody();
    }

    private DiferencaNaParcelaRequest criarRequest() {
        DiferencaNaParcelaRequest request = new DiferencaNaParcelaRequest();
        request.setParametrosDeSeguroPrestamista(
                new ParametrosDeSeguroPrestamista(false, false, true,
                        true, true, BigDecimal.ZERO, BigDecimal.ZERO, true, true, "", Arrays.asList(), null));
        request.setTipoPessoa("pessoa-fisica");
        request.setValorUfEmplacamento(new BigDecimal("278.02"));
        request.setValorTC(new BigDecimal("550"));
        request.setValorCestaServicos(new BigDecimal("500"));
        request.setRetorno("1");
        request.setDataPrimeiroVencimento(LocalDate.of(2019, 02, 16));
        request.setPlanoId(2709);
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setValorBem(new BigDecimal("100000"));
        request.setValorEntrada(new BigDecimal("40000"));
        request.setValorParcelaResidual(new BigDecimal("25000"));
        return request;
    }

    @Test
    public void simulacaoPeloResidual() {
        SimulacaoParcResidualRequest request = criarRequest();
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        assertNotNull(r);
        assertNotNull(r.getBody());
        assertNotNull(r.getBody().getPrazos());
        assertFalse(r.getBody().getPrazos().isEmpty());
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
    }

    @Test
    public void realizaSimulacaoPeloResidualNaoAtingindoValorDesejado() {
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorParcelaDesejada(new BigDecimal("2000"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertFalse(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoNaPrimeiraTentativa() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        request.setValorParcelaDesejada(new BigDecimal("2100"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(2).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoNaSegundaTentativa() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        request.setValorParcelaDesejada(new BigDecimal("3334.90"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(1).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoNaTerceiraTentativa() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        request.setValorParcelaDesejada(new BigDecimal("4936.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom5DesvioAbaixo() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        request.setValorParcelaDesejada(new BigDecimal("3741.36"));
        request.setValorEntrada(new BigDecimal("42000"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    @Ignore
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom6DesvioAbaixoRetornandoSegundoPrazoProcurado() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        // valor do prazo 2.334.90 , deve retornar o segundo prazo

        request.setValorParcelaDesejada(new BigDecimal("2328.9"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        PrazoResidualResponse prazoSugerido = response.getPrazos().stream().filter(p -> p.getFluxo() != null).findFirst().orElse(null);
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        // o valor é atingido considerando que tem uma parcela com valor abaixo da desejada mesmo que fora da tolerância
        Assert.assertTrue(response.isValorAtingido());
        BigDecimal valorParcelaSugerido = prazoSugerido.getValorParcela();
        Assert.assertEquals(new BigDecimal("2013.52"), valorParcelaSugerido);
    }

    @Test
    @Ignore
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom6DesvioAbaixoRetornandoPrimeiroPrazoProcurado() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        // valor do prazo 1.918.00 , deve retornar o primeiro prazo de trás para frente

        request.setValorParcelaDesejada(new BigDecimal("1.912"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        PrazoResidualResponse prazoSugerido = response.getPrazos().stream().filter(p -> p.getFluxo() != null).findFirst().orElse(null);
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertFalse(response.isValorAtingido());
        BigDecimal valorParcelaSugerido = prazoSugerido.getValorParcela();
        Assert.assertEquals(new BigDecimal("2013.52"), valorParcelaSugerido);
    }

    @Test
    @Ignore
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom6DesvioCima() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        // valor do prazo 3.936.15

        request.setValorParcelaDesejada(new BigDecimal("3942.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        PrazoResidualResponse prazoSugerido = response.getPrazos().stream().filter(p -> p.getFluxo() != null).findFirst().orElse(null);
        // o valor é atingido considerando que tem uma parcela com valor abaixo da desejada mesmo que fora da tolerância
        Assert.assertTrue(response.isValorAtingido());
        BigDecimal valorParcelaSugerido = prazoSugerido.getValorParcela();
        Assert.assertEquals(new BigDecimal("2456.91"), valorParcelaSugerido);
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom5DesvioAcima() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        request.setValorParcelaDesejada(new BigDecimal("4941.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }


    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom3DesvioAbaixo() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        //118.0845
        request.setValorParcelaDesejada(new BigDecimal("4936.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom3DesvioAcima() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        request.setValorParcelaDesejada(new BigDecimal("4939.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }


    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom1DesvioAbaixo() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        //118.0845
        request.setValorParcelaDesejada(new BigDecimal("4935.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void realizaSimulacaoPeloResidualAtingindoValorDesejadoCom1DesvioAcima() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        request.setValorParcelaDesejada(new BigDecimal("4936.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(0).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }

    @Test
    public void residualZero() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorParcelaResidual(BigDecimal.ZERO);
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2813);
        request.setValorParcelaDesejada(new BigDecimal("9000"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> assertEquals(SimuladorExceptionController.UM_CENTESIMO, p.getValorParcelaResidual()));
    }

    @Test
    public void testeResidualComPlanoCDC() {
        prepararMockDeSimulacaoServices(Arrays.asList(new Integer[]{12, 24, 36, 48}));
        SimulacaoParcResidualRequest request = criarRequest();
        request.setValorParcelaResidual(null);
        request.setModalidade(Modalidade.CDC);
        request.setParcelas(new Integer[]{12, 24, 36, 48});
        request.setPlanoId(2812);
        //3.936.15
        request.setValorParcelaDesejada(new BigDecimal("3936.15"));
        ResponseEntity<SimulacaoParcResidualResponse> r = controller.getParcelaResidual(request);
        r.getBody().getPrazos().forEach(p -> System.out.println(p.getParcelas() + " - " + p.getValorTotalFinanciado() + " - " + p.getFluxo()));
        SimulacaoParcResidualResponse response = r.getBody();
        long prazosComFluxo = response.getPrazos().stream().filter(p -> p.getFluxo() != null).count();
        Assert.assertEquals(1, prazosComFluxo);
        Assert.assertEquals(3, response.getPrazos().size());
        Assert.assertTrue(response.getPrazos().get(1).getFluxo() != null);
        Assert.assertTrue(response.isValorAtingido());
    }
}
