package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Retorno {

    @Schema(name = "codigo", description = "Código", example = "1")
    private String codigo;

    @Schema(name = "descricao", description = "Descrição do codigo", example = "1")
    private String descricao;

    @Schema(name = "codigo-retorno", description = "Código da simulação (Código de Retorno)", example = "175")
    @JsonProperty("codigo-retorno")
    private String codigoRetorno;
}
