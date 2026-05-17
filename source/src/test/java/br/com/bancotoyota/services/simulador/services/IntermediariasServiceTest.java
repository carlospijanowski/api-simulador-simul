package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.DiferencaNaParcelaRequest;
import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualSimplesRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.controller.completo.MocksParaTestesCompletos;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.services.exceptions.EntityNotFoundException;

import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class IntermediariasServiceTest extends MocksParaTestesCompletos {

    private IntermediariasService service;

    @MockBean
    private FeatureToggleConfig featureToggleConfig;
   

    @Before
    public void setUp() throws IOException {
        super.setUp();
        prepararMockDeSimulacaoServices(Arrays.asList(24, 36, 60));
        service = new IntermediariasService(controller, planoService, simulacaoServices, prestamistaService, featureToggleConfig);
    }

    @Test(expected = EntityNotFoundException.class)
    public void procurandoIntermediariasNaoAtingeValor() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(1000));
        request.setIntermediarias(criarIntermediarias());
        service.encontrarValorDasIntermediariasParaTeste(request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
    }

    @Test
    public void procurandoIntermediariasParcelaDesejadaAlemDoValorMaximoAtingivel() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(4000));
        request.setIntermediarias(criarIntermediarias());
        try {
            service.encontrarValorDasIntermediariasParaTeste(request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
            throw new AssertionFailedError("essa chamada não pode funcionar");
        } catch (BusinessValidationException ex) {
            assertEquals("O valor da parcela sem intermediárias já está abaixo do valor" +
                    " desejado. Não há necessidade de se adicionar intermediárias.", ex.getMessage());
        }
    }

    @Test
    @Ignore
    public void procurandoIntermediariasAtingeComValorMaior() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2100));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("6202.44"), p.getValor()));
    }

    @Test
    @Ignore
    public void procurandoIntermediariasAtingeNoMeio1() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2100));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("6202.44"), p.getValor()));
    }

    @Test
    @Ignore
    public void procurandoIntermediariasAtingeNoMeio2() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2200));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("3817.83"), p.getValor()));
    }

    /**
     * Esse teste precisa gerar o caso de SPF para maior intermediária e SVP para zero de intermediária.
     */
    @Test
    @Ignore
    public void procurandoIntermediariasComPrestamistaSPF() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setParcelas(new Integer[] { 45 });
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2500));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("4932.34"), p.getValor()));
    }

    /**
     * Esse teste precisa gerar o caso de SPF para maior intermediária e SVP para zero de intermediária.
     */
    @Test
    @Ignore
    public void procurandoIntermediariasComPrestamistaSVP() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setParcelas(new Integer[] { 45 });
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2501));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("4014.61"), p.getValor()));
    }

    /**
     * As condições para que o seguro prestamista não possa ser aplicado não variam com o prazo, portanto não há
     * como a simulação sem intermediárias dar um tipo de seguro prestamista diferente do tipo com o valor máximo
     * para a intermediária e ao mesmo tempo um dos tipos ser NENHUM.
     */
    @Test
    @Ignore
    public void procurandoIntermediariasComPrestamistaNenhum() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setValorBem(new BigDecimal(530000));
        request.setValorEntrada(new BigDecimal(150000));
        request.setParcelas(new Integer[] { 45 });
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(13000));
        request.setIntermediarias(criarIntermediarias());
        List<ParcelaIntermediaria> list = service.encontrarValorDasIntermediariasParaTeste(
                request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
        list.forEach(p -> assertEquals(new BigDecimal("17942.18"), p.getValor()));
    }

    @Test
    @Ignore
    public void testeCompleto() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(2020));
        request.setIntermediarias(null);
        List<ParcelaIntermediaria> list = service.criarIntermediarias(request, new BigDecimal(8), new BigDecimal(5));
        assertEquals(3, list.size());
        assertEquals(new BigDecimal("6079.15"), list.get(0).getValor());
        System.out.println("data de cálculo: " + dataCarencia.getDataCalculo());
        System.out.println("primeira parcela: " + request.getDataPrimeiroVencimento() + " - intermediária: " + list.iterator().next());

        try {
            for (int i = 0; i < 4; i++) {
                request.setDataPrimeiroVencimento(request.getDataPrimeiroVencimento().plusMonths(1));
                list = service.criarIntermediarias(request, new BigDecimal(8), new BigDecimal(5));
                System.out.println("primeira parcela: " + request.getDataPrimeiroVencimento() + " - intermediária: " + list.iterator().next());
            }
        } catch (EntityNotFoundException ex) {
            System.out.println(ex.getMessage() + " - " + request.getDataPrimeiroVencimento());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void requestInvalido1() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(3100));
        request.setIntermediarias(criarIntermediarias());
        request.setParcelas(null); // um prazo deve ser passado
        service.encontrarValorDasIntermediariasParaTeste(request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
    }

    @Test(expected = IllegalStateException.class)
    public void requestInvalido2() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setModalidade(Modalidade.CDC);
        request.setValorParcelaResidual(null);
        request.setValorParcelaDesejada(new BigDecimal(3100));
        request.setIntermediarias(criarIntermediarias());
        request.setParcelas(new Integer[]{24, 36}); // somente um prazo deve ser passado
        service.encontrarValorDasIntermediariasParaTeste(request, new BigDecimal(8), new BigDecimal(5), dataCarencia.getDataCalculo());
    }

    private List<ParcelaIntermediaria> criarIntermediarias() {
        List<ParcelaIntermediaria> list = new ArrayList<>();
        ParcelaIntermediaria p = new ParcelaIntermediaria();
        p.setVencimento(LocalDate.of(2019, 12, 16));
        list.add(p);
        p = new ParcelaIntermediaria();
        p.setVencimento(LocalDate.of(2020, 12, 16));
        list.add(p);
        return list;
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
        request.setRetorno("1");
        request.setVlrSeguroFranquia(new BigDecimal(1200));
        request.setDataPrimeiroVencimento(LocalDate.of(2019, 2, 16));
        request.setPlanoId(2806);
        request.setCarenciaDoPlano(new Carencia(10, 180));
        request.setValorBem(new BigDecimal(100000));
        request.setValorEntrada(new BigDecimal(30000));
        request.setValorParcelaResidual(new BigDecimal(25000));
        request.setParcelas(new Integer[]{60});
        return request;
    }

    @Test
    public void criacaoDeIntermediarias() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        List<ParcelaIntermediaria> list = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
        assertNotNull(list);
        assertEquals(3, list.size());
        int ano = request.getDataPrimeiroVencimento().getYear();
        for (ParcelaIntermediaria pi : list) {
            assertEquals(Month.DECEMBER, pi.getVencimento().getMonth());
            assertEquals(ano, pi.getVencimento().getYear());
            ano ++;
        }
    }

    @Test
    public void criacaoDeIntermediariasSemDataDePagamento() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setDataPrimeiroVencimento(null);
        request.setNDiasAposVencimento(true);
        List<ParcelaIntermediaria> list = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
        assertNotNull(list);
        assertEquals(3, list.size());
        int ano = getDataAtual().getYear();
        for (ParcelaIntermediaria pi : list) {
            assertEquals(Month.DECEMBER, pi.getVencimento().getMonth());
            assertEquals(ano, pi.getVencimento().getYear());
            ano ++;
        }
    }

    @Test
    public void criacaoDeIntermediariasSemDataDePagamentoComDataAtualNoLimiteParaMudarParaAnoSeguinte() {
        LocalDate oldDataAtual = dataAtual;
        try {
            dataAtual = dataAtual.plusMonths(5);
            SimulacaoParcResidualSimplesRequest request = criarRequest();
            request.setDataPrimeiroVencimento(null);
            request.setNDiasAposVencimento(true);
            List<ParcelaIntermediaria> list = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
            assertNotNull(list);
            assertEquals(3, list.size());
            // tem que passar para o ano seguinte porque a primeira parcela é no mês 7 e precisamos de um mínimo de
            // 6 meses para a primeira intermediária
            int ano = getDataAtual().getYear() + 1;
            for (ParcelaIntermediaria pi : list) {
                assertEquals(Month.DECEMBER, pi.getVencimento().getMonth());
                assertEquals(ano, pi.getVencimento().getYear());
                ano++;
            }
        } finally {
            dataAtual = oldDataAtual;
        }
    }

    @Test
    public void numeroDeIntermediarias() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        for (int i = 1; i <= 61; i++) {
            request.getParcelas()[0] = i;
            int numero = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo()).size();
            if (i < 13) {
                assertEquals(0, numero);
            } else if (i < 25) {
                assertEquals(1, numero);
            } else if (i < 37) {
                assertEquals(2, numero);
            } else {
                assertEquals(3, numero);
            }
        }
    }

    @Test
    public void primeiraParcelaEmDezembro() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setDataPrimeiroVencimento(LocalDate.of(2019, 12, 16));
        List<ParcelaIntermediaria> list = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
        assertNotNull(list);
        assertEquals(3, list.size());
        // com a primeira parcela no segundo semestre a primeira intermediária fica para o ano seguinte
        int ano = request.getDataPrimeiroVencimento().getYear() + 1;
        for (ParcelaIntermediaria pi : list) {
            assertEquals(Month.DECEMBER, pi.getVencimento().getMonth());
            assertEquals(ano, pi.getVencimento().getYear());
            ano ++;
        }
    }

    @Test
    public void primeiraParcelaEmJulho() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setDataPrimeiroVencimento(LocalDate.of(2019, 7, 1));
        List<ParcelaIntermediaria> list = service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
        assertNotNull(list);
        assertEquals(3, list.size());
        // com a primeira parcela no segundo semestre a primeira intermediária fica para o ano seguinte
        int ano = request.getDataPrimeiroVencimento().getYear() + 1;
        for (ParcelaIntermediaria pi : list) {
            assertEquals(Month.DECEMBER, pi.getVencimento().getMonth());
            assertEquals(ano, pi.getVencimento().getYear());
            ano ++;
        }
    }

    @Test
    public void requestInvalido() {
        SimulacaoParcResidualSimplesRequest request = criarRequest();
        request.setDataPrimeiroVencimento(null);
        try {
            service.criarIntermediariasImpl(request, dataCarencia.getDataCalculo());
            throw new AssertionFailedError("essa chamada não pode funcionar");
        } catch (BusinessValidationException ex) {
            assertEquals("data de pagamento não informada e nem vencimento-n-dias", ex.getMessage());
        }
    }
}
