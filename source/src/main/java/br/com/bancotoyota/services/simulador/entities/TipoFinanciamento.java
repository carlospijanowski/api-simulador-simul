package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Santos
 * Enumerador de tipo de financiamento
 */
public enum TipoFinanciamento {
    CDC("OV"),
    LEASING("LE");

    private String sistema;

    TipoFinanciamento(String sistema) {
        this.sistema = sistema;
    }

    private MessageUtils messageUtils = new MessageUtils(getClass());

    public String getSistema() {
        return sistema;
    }

    public String getDescricao() {
        return messageUtils.getMessage(this);
    }

    public static String[] toStringArray() {
        List<String> tiposFinanciamento = new ArrayList<>();
        for (TipoFinanciamento tipoFinanciamento : values()) {
            tiposFinanciamento.add(tipoFinanciamento.name());
        }
        return tiposFinanciamento.toArray(new String[tiposFinanciamento.size()]);
    }
}
