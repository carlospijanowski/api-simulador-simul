package br.com.bancotoyota.services.simulador.utils;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.format.DateTimeFormatter;

public class LocalDateTimeISOSerializer extends LocalDateTimeSerializer {
    public LocalDateTimeISOSerializer() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
