package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.repository.RedisRepository;
import br.com.bancotoyota.services.simulador.services.MotorTaxaPlanoService;
import br.com.bancotoyota.services.simulador.services.PlanoService;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import org.apache.commons.lang.BooleanUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.HttpMessageConversionException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimulacaoServicesTest {

    private SimulacaoServicesImpl service;

    @Mock
    private RedisRepository redisRepository;

    @Mock
    private PlanoService planoService;

    @Mock
    private MotorTaxaPlanoService motorTaxaPlanoService;

    private FeatureToggleConfig featureToggleConfig = new FeatureToggleConfig(Boolean.TRUE, Boolean.FALSE);

    @Before
    public void setup() {
        service = new SimulacaoServicesImpl(redisRepository, planoService, motorTaxaPlanoService, featureToggleConfig);
    }

    @Test
    public void filtroPorTaxaZero() {
        List<Prazo> lista = new ArrayList<>();
        lista.add(new Prazo(12, BigDecimal.ZERO.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));
        lista.add(new Prazo(13, new BigDecimal("0.01"), new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));

        carregaPrazosByToggle(lista);
        List<Prazo> list = service.getParcelas(1234, BigDecimal.valueOf(25.000000), null, null, null);
        assertEquals("prazos com taxa zero não devem ser retornados", 1, list.size());
    }


    @Test
    public void taxaForcada() {
        List<Prazo> lista = new ArrayList<>();
        lista.add(new Prazo(12, new BigDecimal("0.01"), new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));
        lista.add(new Prazo(13, new BigDecimal("0.01"), new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));

        carregaPrazosByToggle(lista);

        List<Prazo> list = service.getParcelas(1234, BigDecimal.valueOf(25.000000), null, BigDecimal.ONE, null);
        assertEquals(2, list.size());
        list.forEach(prazo -> assertEquals(BigDecimal.ONE, prazo.getTaxaBanco()));
    }

    @Test
    public void quandoPlanoIdNegativoSemArrayDePrazosEntaoLancaBusinessValidationException() {
        BigDecimal percentualEntrada = BigDecimal.valueOf(25.000000);

        try {
            service.getParcelas(-1, percentualEntrada, null, null, null);
            fail("meses e taxaMes são obrigatórios se plano id for -1");
        } catch (BusinessValidationException ex) {
            assertEquals("quando o id do plano não é passado (-1) é necessário especificar o número de parcelas",
                    ex.getMessage());
        }
    }

    @Test
    public void quandoPlanoIdNegativoComArrayDePrazosEntaoLancaBusinessValidationException() {
        BigDecimal percentualEntrada = BigDecimal.valueOf(25.000000);
        List<Integer> prazos = Arrays.asList(14);

        try {
            service.getParcelas(-1, percentualEntrada, prazos, null, null);
            fail("meses e taxaMes são obrigatórios se plano id for -1");
        } catch (BusinessValidationException ex) {
            assertEquals("quando o id do plano não é passado (-1) é necessário especificar a taxa mês",
                    ex.getMessage());
        }
    }

    @Test
    public void semPlanoIdOk() {
        List<Prazo> list = service.getParcelas(-1, BigDecimal.valueOf(25.000000), Arrays.asList(14), BigDecimal.ONE, null);
        assertEquals(1, list.size());
        Prazo prazo = list.iterator().next();
        assertEquals(BigDecimal.ONE, prazo.getTaxaBanco());
        assertEquals(Integer.valueOf(14), prazo.getNumeroDeParcelas());
    }

    @Test
    public void prazoNaoEncontrado() {
        List<Prazo> lista = new ArrayList<>();

        carregaPrazosByToggle(lista);
        Set<Integer> meses = new HashSet<>();
        meses.add(14);
        try {
            service.getParcelas(1234, BigDecimal.valueOf(25.000000), meses, null, null);
        } catch (BusinessValidationException ex) {
            assertEquals("plano não pode ser usado com esse número de parcelas", ex.getMessage());
        }
    }

    @Test
    public void filtroPorPercentualEntrada() {
        List<Prazo> lista = new ArrayList<>();

        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(10), new BigDecimal(30),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(31), new BigDecimal(40),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(41), new BigDecimal(50),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(51), new BigDecimal(60),null));

        carregaPrazosByToggle(lista);

        BigDecimal percEntrada = BigDecimal.valueOf(35);
        List<Prazo> list = service.getParcelas(1234, percEntrada, null, null, null);

        assertEquals("Deve filtrar com base no percentual de entrada", 1, list.size());
        assertTrue(list.get(0).getPercentualMinEntrada().compareTo(percEntrada) <= 0);
        assertTrue(list.get(0).getPercentualMaxEntrada().compareTo(percEntrada) >= 0);
    }

    @Test
    public void filtroPorPercentualEntradaForaDoLimite() {
        List<Prazo> lista = new ArrayList<>();

        lista.add(new Prazo(12, BigDecimal.ONE.setScale(30), new BigDecimal(20), new BigDecimal(50), new BigDecimal(10), new BigDecimal(30),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(31), new BigDecimal(20), new BigDecimal(50), new BigDecimal(31), new BigDecimal(40),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(41), new BigDecimal(50),null));
        lista.add(new Prazo(12, BigDecimal.ONE.setScale(3), new BigDecimal(20), new BigDecimal(50), new BigDecimal(51), new BigDecimal(60),null));

        carregaPrazosByToggle(lista);

        BigDecimal percEntrada = BigDecimal.valueOf(0);
        try {
            service.getParcelas(2709, percEntrada, null, null, null);
        } catch (BusinessValidationException ex) {
            assertEquals("percentual de entrada não se enquadra nos limites do plano", ex.getMessage());
        }
    }

    @Test
    public void filtroPorPercentualEntradaComDecimais() {
        List<Prazo> lista = new ArrayList<>();

        lista.add(new Prazo(30, BigDecimal.ONE.setScale(30), new BigDecimal(20), new BigDecimal(50), new BigDecimal(10), new BigDecimal(30.9999),null));
        lista.add(new Prazo(31, BigDecimal.ONE.setScale(31), new BigDecimal(20), new BigDecimal(50), new BigDecimal(31), new BigDecimal(40),null));

        carregaPrazosByToggle(lista);

        BigDecimal percEntrada = BigDecimal.valueOf(30.9);
        List<Prazo> list = service.getParcelas(1234, percEntrada, null, null, null);

        assertEquals("", 1, list.size());
        assertEquals(Integer.valueOf(30), list.get(0).getNumeroDeParcelas()); //Usando o numero de parcelas como forma de identificação somente

        percEntrada = BigDecimal.valueOf(31);
        list = service.getParcelas(1234, percEntrada, null, null, null);

        assertEquals("", 1, list.size());
        assertEquals(Integer.valueOf(31), list.get(0).getNumeroDeParcelas()); //Usando o numero de parcelas como forma de identificação somente

        percEntrada = BigDecimal.valueOf(40.01);
        try {
            service.getParcelas(1234, percEntrada, null, null, null);
        } catch (BusinessValidationException ex) {
            assertEquals("percentual de entrada não se enquadra nos limites do plano", ex.getMessage());
        }
    }

    @Test
    public void filtroPorPercentualEntradaComPlanosSemRestricao() {
        List<Prazo> lista = new ArrayList<>();

        lista.add(new Prazo(12, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0),null));
        lista.add(new Prazo(36, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(0), new BigDecimal(0),null));
        lista.add(new Prazo(18, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(20), new BigDecimal(50),null));
        lista.add(new Prazo(18, BigDecimal.ONE, new BigDecimal(20), new BigDecimal(50), new BigDecimal(51), new BigDecimal(70),null));

        carregaPrazosByToggle(lista);

        BigDecimal percEntrada = BigDecimal.valueOf(35);
        List<Prazo> list = service.getParcelas(1234, percEntrada, null, null, null);

        assertEquals("Deve trazer planos antigos, sem limite de entrada", 3, list.size());
        assertEquals("Deve trazer planos antigos, sem limite de entrada", 1, list.stream().filter(p -> p.getNumeroDeParcelas() == 18).collect(Collectors.toList()).size());
    }

    @Test(expected = HttpMessageConversionException.class)
    public void getTaxasIOFTipoInvalido() {
        service.getTaxasIOF(null, null);
    }

    @Test
    public void getTaxasIOF() {
        TaxasIOF taxasIOF = new TaxasIOF("365", "0.1", "0.2");
        when(redisRepository.findTaxasIOF("iof:pf")).thenReturn(taxasIOF);
        when(redisRepository.findTaxasIOF("iof:pj")).thenReturn(taxasIOF);
        TaxasIOF t1 = service.getTaxasIOF(TipoPessoa.PESSOA_FISICA, null);
        assertNotNull(t1);
        TaxasIOF t2 = service.getTaxasIOF(TipoPessoa.PESSOA_JURIDICA, null);
        assertNotNull(t2);
        verify(redisRepository).findTaxasIOF("iof:pf");
        verify(redisRepository).findTaxasIOF("iof:pj");
    }

    @Test
    public void quandoRequestComParcelasNaSimulacaoPrestamistaEntaoContinuaFluxo() {
        SimulacaoParcResidualRequest request = mock(SimulacaoParcResidualRequest.class);
        when(request.getParcelas()).thenReturn(new Integer[] {12});
        service.validaRequestSimulacao(request);

        verify(request, times(2)).getParcelas();
    }

    @Test(expected = BusinessValidationException.class)
    public void quandoRequestSemParcelasNaSimulacaoPrestamistaEntaoRetornaErro() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();

        service.validaRequestSimulacao(request);
    }

    @Test(expected = BusinessValidationException.class)
    public void quandoRequestComMaisDeUmaParcelasNaSimulacaoPrestamistaEntaoRetornaErro() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setParcelas(new Integer[] {12, 24});

        service.validaRequestSimulacao(request);
    }

    private void carregaPrazosByToggle(List<Prazo> lista) {
        if(BooleanUtils.isTrue(featureToggleConfig.getBuscaApiMotorTaxaEnabled())) {
            when(motorTaxaPlanoService.findPrazos(any(), any())).then(i -> lista);
        } else {
            when(planoService.findPrazos(any(), any())).then(i -> lista);
        }
    }

}