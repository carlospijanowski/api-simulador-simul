package br.com.bancotoyota.services.simulador.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * @author Luis Santos
 * Classe abstrata para manipulação de valores numericos
 */
public abstract class NumberUtils {

    public static final BigDecimal CEM = new BigDecimal(100);

    private NumberUtils() {}

    private static NumberFormat decimalFormat;

    public static final BigDecimal ZERO_2_CASAS = new BigDecimal("0.00");

    static {
        resetBigDecimalNumberFormat();
    }

    /**
     * Usado para testes apenas para que uma nova instância possa ser criado com um Locale diferente
     */
    static void resetBigDecimalNumberFormat() {
        decimalFormat = NumberFormat.getNumberInstance();
    }

    /**
     * Formata um valor numérico.
     * @param number valor numérico.
     * @return o valor numérico formatado.
     */
    public static String format(Number number) {
        return decimalFormat.format(number);
    }

    /**
     * Formata um valor numérico com o padrão informado.
     * @param number valor numérico.
     * @param pattern padrão de formatação.
     * @return o valor numérico formatado.
     */
    public static String format(Number number, String pattern) {
        return new DecimalFormat(pattern).format(number);
    }

    /**
     * Converte um valor alfanumérico em um valor numérico.
     * @param source valor alfanumérico.
     * @return o valor numérico convertido a partir do valor alfanumérico.
     * @throws ParseException
     */
    public static Number parse(String source) throws ParseException {
        return decimalFormat.parse(source);
    }

    /**
     * Converte um valor alfanumérico em um valor numérico com o padrão informado.
     * @param source valor alfanumérico.
     * @param pattern padrão de formatação.
     * @return o valor numérico convertido a partir do valor alfanumérico.
     * @throws ParseException
     */
    public static Number parse(String source, String pattern) throws ParseException {
        return new DecimalFormat(pattern).parse(source);
    }

    /**
     * Converte um valor alfanumérico em um big decimal.
     * @param value valor alfanumérico.
     * @return o big decimal convertido a partir do valor alfanumérico.
     */
    public static BigDecimal toBigDecimal(String value) {
        if (value == null) {
            return null;
        }

        if (value.equals("NaN")) {
            throw new IllegalStateException("resultado da planilha deu NaN, não é possível trabalhar com tal valor");
        }

        DecimalFormat decFormat = new DecimalFormat();
        DecimalFormatSymbols decSymbols = decFormat.getDecimalFormatSymbols();

        if (decSymbols.getDecimalSeparator() == '.') {
            return new BigDecimal(value.replaceAll(",", ""));
        } else {
            return new BigDecimal(value.replaceAll("\\.", "").replaceAll(",", "."));
        }
    }

    public static Integer toInteger(String value) {
        if (value == null) {
            return null;
        }
        DecimalFormat decFormat = new DecimalFormat();
        DecimalFormatSymbols decSymbols = decFormat.getDecimalFormatSymbols();

        if (decSymbols.getDecimalSeparator() == '.') {
            return Integer.parseInt(value.replaceAll(",", ""));
        } else {
            return Integer.parseInt(value.replaceAll("\\.", "").replaceAll(",", "."));
        }

    }
}
