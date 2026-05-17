package br.com.bancotoyota.services.simulador.beans.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ValorSubsidio implements Serializable {

    @Schema(name = "valor-loja", description = "Valor subsidio da loja.", example = "5100")
    @JsonProperty("valor-loja")
    private BigDecimal loja;

    @Schema(name = "valor-montadora", description = "Valor subsidio da montadora.", example = "5100")
    @JsonProperty("valor-montadora")
    private BigDecimal montadora;
}
