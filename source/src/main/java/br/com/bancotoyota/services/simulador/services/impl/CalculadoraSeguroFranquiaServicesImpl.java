package br.com.bancotoyota.services.simulador.services.impl;

import br.com.bancotoyota.services.simulador.beans.response.SeguroFranquia;
import br.com.bancotoyota.services.simulador.services.CalculadoraSeguroFranquiaServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
@Slf4j
public class CalculadoraSeguroFranquiaServicesImpl implements CalculadoraSeguroFranquiaServices {


    public SeguroFranquia calcular(BigDecimal fator, BigDecimal valorSeguroFranquia) {
        log.info("Iniciando o cálculo do seguro franquia. Fator: {}, Valor Seguro Franquia: {}", fator, valorSeguroFranquia);
         SeguroFranquia seguroFranquia = new SeguroFranquia();
         BigDecimal valorParcela = arredondarParaDuasCasaDescimais(valorSeguroFranquia.multiply(fator));
         seguroFranquia.setValorTotal(arredondarParaDuasCasaDescimais(valorSeguroFranquia));
         seguroFranquia.setValorParcela(arredondarParaDuasCasaDescimais(valorParcela));
        //TODO: ADICIONAR CNPJ SEGURADORA
        //seguroFranquia.setCnpjSeguradora(null)
        log.info("Cálculo do seguro franquia concluído com sucesso - Valor total do seguro: {}, Valor da parcela: {}", seguroFranquia.getValorTotal(), seguroFranquia.getValorParcela());
         return seguroFranquia;
    }

    public BigDecimal arredondarParaDuasCasaDescimais(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
