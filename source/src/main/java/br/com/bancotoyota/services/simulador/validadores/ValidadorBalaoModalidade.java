package br.com.bancotoyota.services.simulador.validadores;

import br.com.bancotoyota.services.simulador.beans.request.ParcelaIntermediaria;
import br.com.bancotoyota.services.simulador.entities.EnumControleBalao;

import java.math.BigDecimal;
import java.util.List;

public abstract class ValidadorBalaoModalidade {

    public List<ParcelaIntermediaria> intermediarias;
    public BigDecimal valorParcelaResidual;
    public EnumControleBalao controleBalao;
    public Boolean permiteBalao;

    public ValidadorBalaoModalidade(List<ParcelaIntermediaria> intermediarias, BigDecimal valorParcelaResidual, EnumControleBalao controleBalao, Boolean permiteBalao) {
        this.intermediarias = intermediarias;
        this.valorParcelaResidual = valorParcelaResidual;
        this.controleBalao = controleBalao;
        this.permiteBalao = permiteBalao;
    }

    public abstract void validar();
}
