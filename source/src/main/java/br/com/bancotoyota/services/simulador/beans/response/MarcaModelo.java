package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.response.TipoMarcaModelo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarcaModelo {
	
	private TipoMarcaModelo tipo;
	
	@JsonProperty("marca-id")
	private String marcaId;
	
	@JsonProperty("marca-descricao")
	private String marcaDescricao;
	
	@JsonProperty("valor-filtro")
	private List<String> valorFiltro;

}
