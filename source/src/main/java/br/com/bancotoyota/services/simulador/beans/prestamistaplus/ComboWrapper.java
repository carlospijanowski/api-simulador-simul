package br.com.bancotoyota.services.simulador.beans.prestamistaplus;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComboWrapper {
	@JsonProperty("combo")
	private Combo combo;

}
