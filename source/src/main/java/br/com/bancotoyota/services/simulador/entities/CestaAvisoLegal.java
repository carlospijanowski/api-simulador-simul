package br.com.bancotoyota.services.simulador.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CestaAvisoLegal {

    private Integer codigo;

    private String descricao;

    private BigDecimal valor;

}
