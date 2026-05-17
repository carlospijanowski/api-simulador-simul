package br.com.bancotoyota.services.simulador.utils;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.time.format.DateTimeFormatter;

public class LocalDateTimeISODeserializer extends LocalDateTimeDeserializer {

    public LocalDateTimeISODeserializer() {
        super(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
