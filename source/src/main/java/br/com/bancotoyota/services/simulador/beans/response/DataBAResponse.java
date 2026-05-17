package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DataBAResponse {

    @Schema(name = "data-calculo", description = "Usado caso queira usar uma taxa fixa e diferente da taxa do plano.", example = "20250101")
    @JsonProperty("data-calculo")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate dataBa;
}
