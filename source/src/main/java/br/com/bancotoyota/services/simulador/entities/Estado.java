package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.utils.LocalDateTimeDeserializer;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Estado {

    @Schema(name = "uf", description = "UF do registro de emplacamento do veículo", example = "PA")
    private String uf;

    @Schema(name = "descricao", description = "Categoria do veículo", example = "Veiculos Pesados")
    private String descricao;

    @Schema(name = "valor", description = "Valor do registro de emplacamento", example = "382.52")
    private BigDecimal valor;

    @Schema(name = "codigo-registro", description = "Codigo do registro de emplacamento", example = "500")
    @JsonProperty("codigo-registro")
    private String codigoRegistro;

    @Schema(name = "data-inicio", description = "Data do registro de emplacamento do veículo", example = "20180126")
    @JsonProperty("data-inicio")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dataInicio;
}
