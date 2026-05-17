package br.com.bancotoyota.services.simulador.entities;


import br.com.bancotoyota.services.simulador.beans.response.TaxasDetalhe;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Taxa {
	
	private Integer id;
    
	private Integer baseline;
    
	private Integer versao;

	@JsonProperty("status-versionamento")
	private String statusVersionamento;
	
	private String descricao;
	
	@JsonProperty("data-inicial-vigencia")
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	private LocalDate dataInicialVigencia;
	
	@JsonProperty("data-final-vigencia")
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
	private LocalDate dataFinalVigencia;
	
	@JsonProperty("prazo-validade")
	private Integer prazoValidade;
	@JsonProperty("taxas-detalhe")
	private List<TaxasDetalhe> taxasDetalhe;

}
