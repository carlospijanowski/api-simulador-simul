package br.com.bancotoyota.services.simulador.controller;

import br.com.bancotoyota.services.simulador.beans.request.SimulacaoParcResidualRequest;
import br.com.bancotoyota.services.simulador.config.FeatureToggleConfig;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.DIRECT;
import static br.com.bancotoyota.services.simulador.entities.EnumOrigemSolicitacao.GERADOR_OFERTA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimulacaoParcResidualTest {

    private FeatureToggleConfig featureToggleConfig;

    private SimulacaoParcResidual simulacaoParcResidual;

    @BeforeEach
    public void setup() {
        featureToggleConfig = mock(FeatureToggleConfig.class);
        CalculadoraSeguroFranquiaServices seguroFranquiaServices = mock(CalculadoraSeguroFranquiaServices.class);

        this.simulacaoParcResidual = new SimulacaoParcResidual(featureToggleConfig, seguroFranquiaServices);

        when(featureToggleConfig.getBuscaApiMotorTaxaEnabled()).thenReturn(Boolean.TRUE);
        when(featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(Boolean.TRUE);
    }

    @Test
    public void deveIndicarParaNaoDefinirIntermediariaParaCicloQuandoAplicacaoDesconhecida() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao("NOVAAPLICACAO");
        request.setValorParcelaDesejada(null);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertFalse(resultado);
    }

    @Test
    public void deveIndicarParaDefinirIntermediariaParaCicloQuandoAplicacaoDirect() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(DIRECT.getValue());
        request.setValorParcelaDesejada(null);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertTrue(resultado);
    }

    @Test
    public void deveIndicarParaDefinirIntermediariaParaCicloQuandoAplicacaoGeradorOferta() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(GERADOR_OFERTA.getValue());
        request.setValorParcelaDesejada(null);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertTrue(resultado);
    }

    @Test
    public void deveIndicarParaNaoDefinirIntermediariaParaCicloQuandoHouverValorParcelaDesejada() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(DIRECT.getValue());
        request.setValorParcelaDesejada(BigDecimal.ONE);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertFalse(resultado);
    }

    @Test
    public void deveIndicarParaNaoDefinirIntermediariaParaCicloQuandoNaoPermiteBalao() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(DIRECT.getValue());
        request.setValorParcelaDesejada(BigDecimal.ONE);
        request.setPermiteBalao(false);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertFalse(resultado);
    }

    @Test
    public void deveIndicarParaDefinirIntermediariaParaCicloQuandoFuncionalidadeHabilitada() {
        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(DIRECT.getValue());
        request.setValorParcelaDesejada(null);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertTrue(resultado);
    }

    @Test
    public void deveIndicarParaDefinirIntermediariaParaCicloQuandoFuncionalidadeNaoHabilitada() {
        when(featureToggleConfig.getCicloIntermediariasEnabled()).thenReturn(Boolean.FALSE);

        SimulacaoParcResidualRequest request = new SimulacaoParcResidualRequest();
        request.setOrigemSolicitacao(DIRECT.getValue());
        request.setValorParcelaDesejada(null);
        request.setPermiteBalao(true);
        request.setModalidade(Modalidade.CICLO_TOYOTA);

        boolean resultado = this.simulacaoParcResidual.isMontaIntermediariaQuandoForCiclo(request);

        Assertions.assertFalse(resultado);
    }
}
