package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SeguroGarantiaRequest {
    @Schema(name = "marca", description = "Marca do bem financiado", example = "TOYOTA")
    private String marca;

    @Schema(name = "modelo", description = "Modelo do bem financiado", example = "YARIS XL PLUS CON. SED. 1.5 FLEX 16V AUT")
    private String modelo;

    @Schema(name = "ano-modelo-veiculo", description = "Ano modelo do bem financiado", example = "2024")
    @JsonProperty("ano-modelo-veiculo")
    private String anoModelo;

    @Schema(name = "codigo-fipe", description = "Codigo da FIBE do bem financiado", example = "0022071")
    @JsonProperty("codigo-fipe")
    private String codigoFipe;

    @Schema(name = "data-inicio-garantia", description = "Data de inicio do seguro garantia", example = "false")
    @JsonProperty("data-inicio-garantia")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate dataInicioGarantia;

    @Schema(name = "possui-garantia", description = "Indica se o cliente já possui garantia", example = "false")
    @JsonProperty("possui-garantia")
    private Boolean possuiGarantia;

    @Schema(name = "ano-fabricacao-veiculo", description = "Ano de fabricação do veículo", example = "2019")
    @JsonProperty("ano-fabricacao-veiculo")
    private Integer anoFabricacao;

    @Schema(name = "quilometragem", description = "Quilometragem do veículo", example = "12375.12")
    private String quilometragem;
}
