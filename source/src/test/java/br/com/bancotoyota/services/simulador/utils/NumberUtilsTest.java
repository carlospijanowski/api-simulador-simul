package br.com.bancotoyota.services.simulador.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static org.junit.Assert.*;

public class NumberUtilsTest {

    private Locale localeOriginal;

    @Before
    public void setUp() {
        localeOriginal = Locale.getDefault();
    }

    @After
    public void shutDwon() {
        Locale.setDefault(localeOriginal);
    }

    /**
     * Esse teste mostra que o toString do BigDecimal não respeita a configuração de Locale
     */
    @Test
    public void testarToStringDoBigDecimal() {
        double numero = 1.2;
        BigDecimal valorDouble = new BigDecimal(numero);
        System.out.println(valorDouble);
        BigDecimal valor = new BigDecimal("1.20");
        System.out.println(Locale.getDefault());

        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        System.out.println(Locale.getDefault());
        NumberFormat format = NumberFormat.getNumberInstance();
        String strFormatPtBR = format.format(valor);
        System.out.println("NumberFormat: " + strFormatPtBR);
        String strPtBR = valor.toString();
        System.out.println("toString: " + strPtBR);

        Locale.setDefault(Locale.forLanguageTag("en-US"));
        System.out.println(Locale.getDefault());
        format = NumberFormat.getNumberInstance();
        String strFormatEnUS = format.format(valor);
        System.out.println("NumberFormat: " + strFormatEnUS);
        String strEnUS = valor.toString();
        System.out.println("toString: " + strEnUS);

        assertEquals(strEnUS, strPtBR);
        assertNotEquals(strFormatEnUS, strFormatPtBR);
    }

    @Test
    public void testarToBigDecimal() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        NumberUtils.resetBigDecimalNumberFormat();
        BigDecimal valor = NumberUtils.toBigDecimal("1,2");
        assertEquals(new BigDecimal("1.2"), valor);

        Locale.setDefault(Locale.forLanguageTag("en-US"));
        NumberUtils.resetBigDecimalNumberFormat();
        BigDecimal valor2 = NumberUtils.toBigDecimal("1.2");
        assertEquals(new BigDecimal("1.2"), valor2);
    }

    @Test
    public void testarComNull() {
        BigDecimal valor = NumberUtils.toBigDecimal(null);
        assertNull(valor);
    }

    @Test
    public void testaFormat() {
        BigDecimal valor = new BigDecimal("1.2");
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        NumberUtils.resetBigDecimalNumberFormat();
        System.out.println(Locale.getDefault());
        String str = NumberUtils.format(valor);
        System.out.println(str);
        BigDecimal novoValor = NumberUtils.toBigDecimal(str);
        assertTrue("valor deve ser igual " + valor, valor.compareTo(novoValor) == 0);

        Locale.setDefault(Locale.forLanguageTag("en-US"));
        NumberUtils.resetBigDecimalNumberFormat();
        System.out.println(Locale.getDefault());
        str = NumberUtils.format(valor);
        System.out.println(str);
        novoValor = NumberUtils.toBigDecimal(str);
        assertTrue("valor deve ser igual " + valor, valor.compareTo(novoValor) == 0);
    }
}
