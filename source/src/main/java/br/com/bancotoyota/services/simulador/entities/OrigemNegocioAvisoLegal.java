package br.com.bancotoyota.services.simulador.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrigemNegocioAvisoLegal {

    private String codigo;

    private String cnpj;

    private String cidade;

    private String estado;

}
