package br.com.bancotoyota.services.simulador.entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VeiculosResponse {

	private ResponseVeiculos response;
	
	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static class ResponseVeiculos{
		private List<ModeloVeiculos> modelos;	
	}
	
	
}
