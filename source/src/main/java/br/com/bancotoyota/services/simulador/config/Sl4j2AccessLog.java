package br.com.bancotoyota.services.simulador.config;

import org.apache.catalina.valves.AbstractAccessLogValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringWriter;

@Slf4j
public class Sl4j2AccessLog extends AbstractAccessLogValve {

    private Logger accessLog = LoggerFactory.getLogger("access-log");

    @Override
    protected void log(CharArrayWriter message) {
        try {
            StringWriter stringWriter = new StringWriter();
            message.writeTo(stringWriter);
            accessLog.info(stringWriter.toString());
        } catch (IOException ioe) {
            log.warn(sm.getString(
                    "accessLogValve.writeFail", message.toString()), ioe);
        }
    }
}
