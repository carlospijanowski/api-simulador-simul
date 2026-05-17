package br.com.bancotoyota.services.simulador.beans;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SeguroMecanicaSelecionado implements Serializable {
    @Schema(name = "id", description = "Codigo do seguro", example = "2")
    private String id;
    @Schema(name = "prazo", description = "Prazo do seguro", example = "12")
    private Integer prazo;


}
