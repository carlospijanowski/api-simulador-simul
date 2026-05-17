package br.com.bancotoyota.services.simulador.entities;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import br.com.bancotoyota.services.simulador.utils.LocalDateTimeDeserializer;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Document(collection = "simulacao")
public class SimulacaoSimplificada {

    @Schema(name = "codigo-simulacao", description = "Código da simulação.", example = "674a55c2f4e9e5509f6cd5ba")
	@Id
	@JsonProperty("codigo-simulacao")
	private String id;

    @Schema(name = "data-criacao", description = "Data da criação da simulação.", example = "2024-11-29 21:01:06.892")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonProperty("data-criacao")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	private LocalDateTime dataCriacao;

    @Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação", example ="OBO" )
    @JsonProperty("origem-solicitacao")
    private String origemSolicitacao;

    @Schema(name = "descricao-origem-solicitacao", description = "Nome da origem solicitação", example ="SITE BTB" )
    @JsonProperty("descricao-origem-solicitacao")
    private String descricaoOrigemSolicitacao;

    @Schema(name = "session-id", description = "Identificador da sessão do direct", example ="333133-35464-343uifadbe4d" )
    @Indexed
    @JsonProperty("session-id")
    private String sessionId;
    
    @JsonProperty("entrada")
    private Entrada entrada;
    
    @JsonProperty("saida")
    private Saida saida;

    @Schema(name = "tipo-simulacao", description = "Tipo da simulação", example ="SIMPLIFICADA-RESIDUAL" )
    @JsonProperty("tipo-simulacao")
    private TipoSimulacao tipoSimulacao;    
    	
}
