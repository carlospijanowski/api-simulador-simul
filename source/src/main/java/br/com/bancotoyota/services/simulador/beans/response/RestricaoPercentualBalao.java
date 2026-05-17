package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestricaoPercentualBalao extends PercentualBalao {

    @Schema(name = "quantidade-intermediarias-range", description = "Distância entre uma intermediaria e outra", example = "1")
    @JsonProperty("quantidade-intermediarias-range")
    private Integer quantidadeIntermediariasRange;

}
