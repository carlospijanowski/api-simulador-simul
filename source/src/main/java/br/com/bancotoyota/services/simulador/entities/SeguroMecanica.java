package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SeguroMecanica {
    @Schema(name = "id", description = "Codigo do seguro.", example = "1")
    private String id;

    @Schema(name = "descricao", description = "Descrição do seguro.", example = "Teste")
    private String descricao;

    @Schema(name = "valor-total", description = "Valor total do seguro.", example = "2300.21")
    @JsonProperty("valor-total")
    private BigDecimal valorTotal;

    @Schema(name = "valor-parcela", description = "Valor do seguro na parcela.", example = "230.21")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    @Schema(name = "prazo", description = "Prazo do seguro.", example = "21")
    private Number prazo;

    @Schema(name = "cnpj-seguradora", description = "CPNJ da seguradora.", example = "33016221000107")
    @JsonProperty("cnpj-seguradora")
    private String cnpjSeguradora;

    @Schema(name = "data-inicio-seguro-mecanica", description = "Data de início.", example = "20180126")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-inicio-seguro-mecanica")
    private LocalDate dataInicioSeguroMecanica;

    @Schema(name = "quilometragem", description = "Quilometragem do veiculo.", example = "33016221000107")
    private String quilometragem;

    @Schema(name = "tipo", description = "Tipo do seguro", example = "extra")
    @JsonProperty("tipo")
    private String tipo;
}
