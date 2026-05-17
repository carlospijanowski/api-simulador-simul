package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.response.ValorSubsidio;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SubsidioTest {

    @Test
    public void distribuicaoSubsidioFixo() {
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.FIXO)
                .fixoPercentualRevendedora(new BigDecimal(50))
                .fixoPercentualMontadora(new BigDecimal(50))
                .build();
        ValorSubsidio valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(10000));
        assertEquals(new BigDecimal("5000.00"), valorSubsidio.getMontadora());
        assertEquals(new BigDecimal("5000.00"), valorSubsidio.getLoja());
    }

    @Test
    public void distribuicaoVariavelPorPercentualELimiteDeValor() {
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.VALOR_PERCENTUAL)
                .vpPercentualDistrMarca(new BigDecimal(50))
                .vpValorMaxMarca(new BigDecimal(2000))
                .build();

        ValorSubsidio valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(3000));
        assertEquals(new BigDecimal("1500.00"), valorSubsidio.getMontadora());
        assertEquals(new BigDecimal("1500.00"), valorSubsidio.getLoja());

        valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(5000));
        assertEquals(new BigDecimal("2000.00"), valorSubsidio.getMontadora());
        assertEquals(new BigDecimal("3000.00"), valorSubsidio.getLoja());
    }

    @Test
    public void distribuicaoVariavelPorFaixaDeValor() {
        List<Subsidio.FaixaValor> faixas = new ArrayList<>();
        faixas.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal(1000)));
        faixas.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("1000.01"), new BigDecimal(2000)));
        faixas.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("2000.01"), new BigDecimal(3000)));
        faixas.add(new Subsidio.FaixaValor(3, EnumResponsabilidade.MONTADORA, new BigDecimal("3000.01"), new BigDecimal(99999999)));
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.FAIXA_VALOR)
                .faixasValor(faixas)
                .build();

        try {
            subsidio.fazerDistribuicao(new BigDecimal(0));
            fail();
        } catch (BusinessValidationException ex) {
            assertEquals("Bug do subsídio negativo na planilha, use uma data de primeiro pagamento mais para frente", ex.getMessage());
        }

        try {
            subsidio.fazerDistribuicao(new BigDecimal(-10000));
            fail();
        } catch (BusinessValidationException ex) {
            assertEquals("Bug do subsídio negativo na planilha, use uma data de primeiro pagamento mais para frente", ex.getMessage());
        }

        ValorSubsidio valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(10000));
        assertEquals(new BigDecimal("2000.00"), valorSubsidio.getLoja());
        assertEquals(new BigDecimal("8000.00"), valorSubsidio.getMontadora());

        valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(2500));
        assertEquals(new BigDecimal("1500.00"), valorSubsidio.getLoja());
        assertEquals(new BigDecimal("1000.00"), valorSubsidio.getMontadora());

        valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal(1000));
        assertEquals(new BigDecimal("1000.00"), valorSubsidio.getLoja());
        assertEquals(new BigDecimal("0.00"), valorSubsidio.getMontadora());

        valorSubsidio = subsidio.fazerDistribuicao(new BigDecimal("1000.01"));
        assertEquals(new BigDecimal("1000.00"), valorSubsidio.getLoja());
        assertEquals(new BigDecimal("0.01"), valorSubsidio.getMontadora());
    }

    @Test
    public void distribuicaoVariavelPorFaixaDeValorInconsistente() {
        List<Subsidio.FaixaValor> faixas = new ArrayList<>();
        faixas.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal(1000)));
        faixas.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("1000.01"), new BigDecimal(2000)));
        faixas.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("2000.01"), new BigDecimal(3000)));
        faixas.add(new Subsidio.FaixaValor(3, EnumResponsabilidade.MONTADORA, new BigDecimal("3000.01"), new BigDecimal(4000)));
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.FAIXA_VALOR)
                .faixasValor(faixas)
                .build();

        try {
            subsidio.fazerDistribuicao(new BigDecimal(10000));
            fail();
        } catch (BusinessValidationException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void distribuicaoVariavelPorFaixaDeValorEquivalenciaDeFaixasFuradas() {
        List<Subsidio.FaixaValor> faixas = new ArrayList<>();
        faixas.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal(1000)));
        faixas.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("1000.01"), new BigDecimal(2000)));
        faixas.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("3000.01"), new BigDecimal(4000)));
        faixas.add(new Subsidio.FaixaValor(3, EnumResponsabilidade.MONTADORA, new BigDecimal("4000.01"), new BigDecimal(999999999)));
        Subsidio subsidio = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.FAIXA_VALOR)
                .faixasValor(faixas)
                .build();

        ValorSubsidio valorSubsidio1 = subsidio.fazerDistribuicao(new BigDecimal(10000));

        List<Subsidio.FaixaValor> faixas2 = new ArrayList<>();
        faixas.add(new Subsidio.FaixaValor(0, EnumResponsabilidade.DEALER, new BigDecimal("0.01"), new BigDecimal(1000)));
        faixas.add(new Subsidio.FaixaValor(1, EnumResponsabilidade.MONTADORA, new BigDecimal("1000.01"), new BigDecimal(2000)));
        faixas.add(new Subsidio.FaixaValor(2, EnumResponsabilidade.DEALER, new BigDecimal("2000.01"), new BigDecimal(4000)));
        faixas.add(new Subsidio.FaixaValor(3, EnumResponsabilidade.MONTADORA, new BigDecimal("4000.01"), new BigDecimal(999999999)));
        Subsidio subsidio2 = Subsidio.builder()
                .tipoSubsidio(EnumTipoSubsidio.VARIAVEL)
                .tipoDistribuicao(EnumTipoDistribuicao.FAIXA_VALOR)
                .faixasValor(faixas)
                .build();
        ValorSubsidio valorSubsidio2 = subsidio2.fazerDistribuicao(new BigDecimal(10000));
        assertEquals(valorSubsidio2, valorSubsidio1);
    }

}