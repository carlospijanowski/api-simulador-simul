package br.com.bancotoyota.services.simulador.repository;

import br.com.bancotoyota.services.simulador.entities.ControlesBA;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.TaxasIOF;

public interface RedisRepository {
    TaxasIOF findTaxasIOF(String key);
    Seguro getSeguro(String codigo);
    ControlesBA getDataBA();
}
