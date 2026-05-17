package br.com.bancotoyota.services.simulador.services;

import br.com.bancotoyota.services.simulador.beans.CustoPercentualDoSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.DadosCalculo;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConversionException;

import java.math.BigDecimal;

class SeguroPrestamistaCalculoServiceTest {

    @Test
    void deveResolverStrategyPorTipoPessoa() {
        SeguroPrestamistaStrategyResolver resolver = new SeguroPrestamistaStrategyResolver();

        Assertions.assertTrue(resolver.resolve(TipoPessoa.PESSOA_FISICA) instanceof SeguroPrestamistaPessoaFisicaStrategy);
        Assertions.assertTrue(resolver.resolve(TipoPessoa.PESSOA_JURIDICA) instanceof SeguroPrestamistaPessoaJuridicaStrategy);
        Assertions.assertTrue(resolver.resolve(null) instanceof SeguroPrestamistaPessoaFisicaStrategy);
    }

    @Test
    void deveRetornarNenhumParaPessoaJuridica() {
        SeguroPrestamistaCalculoService service = new SeguroPrestamistaCalculoService();

        CustoPercentualDoSeguroPrestamista custo = service.getFatorDoSeguro(null, null,
                BigDecimal.TEN, BigDecimal.ONE, 30, TipoPessoa.PESSOA_JURIDICA);

        Assertions.assertEquals(BigDecimal.ZERO, custo.getFator());
        Assertions.assertEquals(TipoDeSeguroPrestamista.NENHUM, custo.getTipo());
    }

    @Test
    void deveManterComportamentoAtualParaPessoaFisicaQuandoParametrosNaoForemInformados() {
        SeguroPrestamistaCalculoService service = new SeguroPrestamistaCalculoService();
        DadosCalculo dadosCalculo = new DadosCalculo(null, null, null, null, null, null, null);

        Assertions.assertThrows(HttpMessageConversionException.class, () -> service.getFatorDoSeguro(dadosCalculo, null,
                BigDecimal.TEN, BigDecimal.ONE, 30, TipoPessoa.PESSOA_FISICA));
    }
}
