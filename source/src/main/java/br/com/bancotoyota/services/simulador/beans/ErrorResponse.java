package br.com.bancotoyota.services.simulador.beans;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Santos
 * Bean com os dados de erro dos web services.
 */
public class ErrorResponse {

	@Schema(name = "erros", description = "Lista de erros.", example = "[\"Entidade não encontrada. Erro: plano não encontrado(a)!\"]")
	private List<String> erros = new ArrayList<>();

	    public ErrorResponse() {
	        super();
	    }
	    
	    public ErrorResponse(String erro) {
	        this();
	        this.erros.add(erro);
	    }
	    
	    public ErrorResponse(List<String> erros) {
	        this();
	        this.erros.addAll(erros);
	    }

	    public List<String> getErros() {
	        return erros;
	    }
}
