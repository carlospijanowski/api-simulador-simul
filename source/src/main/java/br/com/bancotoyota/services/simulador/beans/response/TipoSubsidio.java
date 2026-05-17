package br.com.bancotoyota.services.simulador.beans.response;


import br.com.bancotoyota.services.simulador.services.exceptions.InvalidFieldException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TipoSubsidio {

	FIXO("F", "FIXO"),
	VARIAVEL("V", "FLEXIVEL/VARIAVEL");

	private String key;
	private String value;
	private String descricao;

	TipoSubsidio(String k, String v) {
		this.key = k;
		this.value = v;
	}
	@JsonCreator(mode = JsonCreator.Mode.DELEGATING)
	public static TipoSubsidio from(String value) {
		try {
			//Verifica se a string tem o valor do nome do construtor.
			return TipoSubsidio.valueOf(value);
		}catch (Exception ex) {
			for (TipoSubsidio e : values()) {
				//Verifica se a string tem o valor do value;
				if (e.value.equalsIgnoreCase(value)) {
					return e;
					//Verifica se a string tem o valor da key;
				}else if (e.key.equalsIgnoreCase(value)) {
					return e;
				}
			}
			throw new InvalidFieldException("tipo-subsidio", value);
		}
	}
	
}
