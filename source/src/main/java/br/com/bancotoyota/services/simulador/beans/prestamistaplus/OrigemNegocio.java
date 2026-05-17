package br.com.bancotoyota.services.simulador.beans.prestamistaplus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrigemNegocio {
	public String codigo;
	public String cnpj;
	public String nome;
}
