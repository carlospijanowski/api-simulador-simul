package br.com.bancotoyota.services.simulador.validadores;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.beans.response.RestricaoPercentualBalao;
import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import br.com.bancotoyota.services.simulador.utils.ExecutorValidadores;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ValidadorIntermediariasCicloTest {

    private ValidadorIntermediariasCiclo validadorIntermediariasCiclo;
    private SimulacaoParcResidualRequest request;
    private List<ValidadorBalaoModalidade> validadores;
    private List<RestricaoPercentualBalao> restricoes;
    private List<Prazo> prazos;
    private List<ParcelaIntermediaria> intermediarias;
    private RestricaoPercentualBalao restricao;

    @Before
    public void setup() {

        validadores = new ArrayList<>();
        restricoes = new ArrayList<>();
        request = new SimulacaoParcResidualRequest();
        request.setDataPrimeiroVencimento(LocalDate.now());

        prazos = new ArrayList<Prazo>() {{
            add(new Prazo(12,
                    new BigDecimal(1.49),
                    new BigDecimal(0),
                    new BigDecimal(100),
                    new BigDecimal(0),
                    new BigDecimal(100),
                    null));
        }};
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 1, request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2, request.getDataPrimeiroVencimento().plusMonths(2)));
        }};

        request.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
        request.setModalidade(Modalidade.CICLO_TOYOTA);
        request.setValorBem(new BigDecimal(100000));
        request.setValorEntrada(new BigDecimal(20000));
        request.setPermiteBalao(true);
        request.setIntervaloResidual(6);

        Integer[] parcelas = {12};

        request.setParcelas(parcelas);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);


    }

    @Test
    public void validarRestricoesQuantidadeExecedida() {

        request.setQuantidadeIntermediarias(1);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);
        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });


    }

    @Test
    public void validarRestricoesQuantidadeExecedidaContraProva() {

        request.setQuantidadeIntermediarias(2);
        intermediarias.add(new ParcelaIntermediaria(new BigDecimal(10000), 12,request.getDataPrimeiroVencimento().plusMonths(11)));

        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);

        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });

    }

    @Test
    public void validarPercentualMinimoInvalido() {

        intermediarias.clear();
        restricoes.clear();

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 12,request.getDataPrimeiroVencimento().plusMonths(11)));
            add(new ParcelaIntermediaria(new BigDecimal(3000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
        }};


        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(2);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });

    }

    @Test
    public void validarPercentualMinimoInvalidoContraProva() {

        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {12};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);
        request.setQuantidadeIntermediarias(2);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(3000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 12, request.getDataPrimeiroVencimento().plusMonths(11)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(3));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarIntervaloResidual() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {36};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(50000), 3, request.getDataPrimeiroVencimento().plusMonths(2)));
            add(new ParcelaIntermediaria(new BigDecimal(1250), 34, request.getDataPrimeiroVencimento().plusMonths(33)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(2);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });

    }

    @Test
    public void validarIntervaloResidualContraProva() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {36};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(50000), 3, request.getDataPrimeiroVencimento().plusMonths(2)));
            add(new ParcelaIntermediaria(new BigDecimal(1250), 29, request.getDataPrimeiroVencimento().plusMonths(28)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(2);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarValorMuitoAltoSemRestricoesContraProva() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {36};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(60000), 29, request.getDataPrimeiroVencimento().plusMonths(28)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(2);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarExcessoIntermediariasSemRestricoes() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {36};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(20000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(15000), 28, request.getDataPrimeiroVencimento().plusMonths(27)));
            add(new ParcelaIntermediaria(new BigDecimal(15000), 29, request.getDataPrimeiroVencimento().plusMonths(28)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(2);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });

    }


    @Test
    public void validarExcessoIntermediariasSemRestricoesContraProva() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {36};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(20000), 2, request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(15000), 28, request.getDataPrimeiroVencimento().plusMonths(27)));
            add(new ParcelaIntermediaria(new BigDecimal(15000), 29, request.getDataPrimeiroVencimento().plusMonths(28)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarIntermediariasParaDozeMeses() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {12};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);
        request.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);

        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(20000), 6, request.getDataPrimeiroVencimento().plusMonths(5)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(50));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });
    }

    @Test
    public void validarIntermediariasComRestricoesNulaEIntevaloInvalido() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {12};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);
        request.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 36, request.getDataPrimeiroVencimento().plusMonths(35)));
            add(new ParcelaIntermediaria(new BigDecimal(20000), 6, request.getDataPrimeiroVencimento().plusMonths(5)));
        }};

        request.setRestricoesPercentuaisBalao(null);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(4);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });
    }

    @Test
    public void validarIntermediariasExcedidoLimiteQuantidadeMaxima() {

        intermediarias.clear();
        restricoes.clear();
        Integer[] parcelas = {12};

        request.setParcelas(parcelas);
        request.setIntervaloResidual(6);
        request.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 12, request.getDataPrimeiroVencimento().plusMonths(11)));
            add(new ParcelaIntermediaria(new BigDecimal(20000), 6, request.getDataPrimeiroVencimento().plusMonths(5)));
        }};

        request.setRestricoesPercentuaisBalao(null);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(1);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });
    }

    @Test
    public void validarPercentualLivresAntesEDepois() {

        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {19};

        request.setParcelas(parcelas);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 1,request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2,request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 3,request.getDataPrimeiroVencimento().plusMonths(2)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 4,request.getDataPrimeiroVencimento().plusMonths(3)));
            add(new ParcelaIntermediaria(new BigDecimal(13000), 19,request.getDataPrimeiroVencimento().plusMonths(18)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(13);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(25);
        restricao.setPrazoFinal(36);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(6);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });

    }

    @Test
    public void validarPercentualLivresAntesEDepoisContraProva() {

        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {19};

        request.setParcelas(parcelas);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 1,request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2,request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 3,request.getDataPrimeiroVencimento().plusMonths(2)));
             add(new ParcelaIntermediaria(new BigDecimal(13000), 19,request.getDataPrimeiroVencimento().plusMonths(18)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(13);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(25);
        restricao.setPrazoFinal(36);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(6);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarPercentualLivresAntesEDepoisPrazo48() {

        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {48};

        request.setParcelas(parcelas);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(10000), 1,request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 2,request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 3,request.getDataPrimeiroVencimento().plusMonths(2)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 40,request.getDataPrimeiroVencimento().plusMonths(39)));
            add(new ParcelaIntermediaria(new BigDecimal(13000), 48,request.getDataPrimeiroVencimento().plusMonths(47)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(13);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(25);
        restricao.setPrazoFinal(36);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(6);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        assertThrows(BusinessValidationException.class, () -> {
            ExecutorValidadores.executarValidadoresBalao(validadores);
        });
    }

    @Test
    public void validarPercentualLivresAntes() {

        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {48};

        request.setParcelas(parcelas);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(1000), 1,request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(2000), 2,request.getDataPrimeiroVencimento().plusMonths(1)));
            add(new ParcelaIntermediaria(new BigDecimal(3000), 3,request.getDataPrimeiroVencimento().plusMonths(2)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 13,request.getDataPrimeiroVencimento().plusMonths(12)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 25,request.getDataPrimeiroVencimento().plusMonths(24)));
            add(new ParcelaIntermediaria(new BigDecimal(13000), 48,request.getDataPrimeiroVencimento().plusMonths(47)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(13);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(25);
        restricao.setPrazoFinal(36);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(30));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        request.setRestricoesPercentuaisBalao(restricoes);

        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(6);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);

    }

    @Test
    public void validarPercentualPrazoForaDoRange() {
        intermediarias.clear();
        restricoes.clear();

        Integer[] parcelas = {24};

        request.setParcelas(parcelas);
        intermediarias = new ArrayList<ParcelaIntermediaria>() {{
            add(new ParcelaIntermediaria(new BigDecimal(11000), 1,request.getDataPrimeiroVencimento()));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 16,request.getDataPrimeiroVencimento().plusMonths(12)));
            add(new ParcelaIntermediaria(new BigDecimal(10000), 24,request.getDataPrimeiroVencimento().plusMonths(23)));
        }};

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(1);
        restricao.setPrazoFinal(12);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(11));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(13);
        restricao.setPrazoFinal(24);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(12));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(25);
        restricao.setPrazoFinal(36);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(13));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);

        restricao = new RestricaoPercentualBalao();
        restricao.setPrazoInicial(37);
        restricao.setPrazoFinal(48);
        restricao.setMinimo(new BigDecimal(10));
        restricao.setMaximo(new BigDecimal(15));
        restricao.setQuantidadeIntermediariasRange(1);

        restricoes.add(restricao);
        request.setControleBalao(EnumControleBalao.OBRIGATORIO_ULTIMA_PARCELA);
        request.setRestricoesPercentuaisBalao(restricoes);
        request.setIntervaloResidual(6);
        request.setIntermediarias(intermediarias);
        request.setQuantidadeIntermediarias(6);
        validadorIntermediariasCiclo = new ValidadorIntermediariasCiclo(request, prazos);
        validadores.add(validadorIntermediariasCiclo);

        ExecutorValidadores.executarValidadoresBalao(validadores);
        assertTrue(true);
    }
}
