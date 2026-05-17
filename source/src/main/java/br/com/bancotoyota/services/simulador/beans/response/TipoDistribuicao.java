package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum TipoDistribuicao {

	PERCENTUAL("V", "VALOR/PERCENTUAL"),
	FAIXA("F", "FAIXA DE VALOR");

	private String key;
	private String value;

	TipoDistribuicao(String k, String v){
		this.key = k;
		this.value = v;
	}

	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static TipoDistribuicao from(String value){
		try {
			//Verifica se a string tem o valor do nome do construtor.
			return TipoDistribuicao.valueOf(value);
		}catch (Exception ex) {
			for (TipoDistribuicao e : values()) {
				//Verifica se a string tem o valor do value;
				if (e.value.equalsIgnoreCase(value)) {
					return e;
					//Verifica se a string tem o valor da key;
				}else if (e.key.equalsIgnoreCase(value)) {
					return e;
				}
			}
			throw new InvalidFieldException("tipo-distribuicao", value);
		}
	}
	
}
