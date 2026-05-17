package br.com.bancotoyota.services.simulador.beans.seguroautointegrado;

import br.com.bancotoyota.services.simulador.beans.geolocalizacao.ParametrizacaoCobrancaGeo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeguroAutoIntegrado {
    @Schema(name = "cnpj-seguradora", description = "CPNJ da seguradora.", example = "33016221000107")
    @JsonProperty("cnpj-seguradora")
    private String cnpjSeguradora;

    @Schema(name = "id-cotacao", description = "Identificador da cotação.", example = "490647149")
    @JsonProperty("id-cotacao")
    private String idCotacao;

    @Schema(name = "prazo-contratado", description = "Prazo do seguro.", example = "12")
    @JsonProperty("prazo-contratado")
    private Integer prazoContratado = 0;
}