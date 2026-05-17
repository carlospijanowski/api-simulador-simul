package br.com.bancotoyota.services.simulador.beans.prestamistaplus;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Combo {
	@JsonProperty("descricao")
	public String descricao;
	@JsonProperty("lojas")
	public List<OrigemNegocio> lojas;
	@JsonProperty("itens")
	public List<Iten> itens;
	@JsonProperty("combo-id")
	public int comboId;
	@JsonProperty("combo-default")
	public boolean comboDefault;
	
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	@JsonProperty("data-inicio")
	public LocalDate dataInicio;
}
