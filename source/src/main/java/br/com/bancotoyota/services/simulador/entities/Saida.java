package br.com.bancotoyota.services.simulador.entities;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class Saida {

    @Schema(name = "data-base", description = "Data da base (BA)", example = "20241027")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-base")
    private LocalDate dataBase;


    @Schema(name = "data-pagamento-parcelas", description = "Data de pagamento da parcela", example = "20241027")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-pagamento-parcelas")
    private LocalDate diaPagamentoParcelas;

    @Schema(name = "codigo-retorno", description = "Código da simulação (Código de Retorno)", example = "1")
    @JsonProperty("codigo-retorno")
    private String codigoRetorno;

    @Schema(name = "tipo", description = "Tipo da simulação", example = "SIMPLIFICADA-RESIDUAL")
    @JsonProperty("tipo")
    private String tipo;    
    
	@JsonProperty("plano")
	private Plano plano;
	
	@JsonProperty("cesta-servicos")
	private CestaServico cestaServico;
	
	@JsonProperty("uf-emplacamento")
	private Estado emplacamento;
	
	@JsonProperty("simulacao")
	private SimulacaoSimplificadaSaida simulacao;
	
	@JsonProperty("parametros-seguro-prestamista")
	private ParametrosDeSeguroPrestamista parametrosSeguroPrestamista;
	
	
}
