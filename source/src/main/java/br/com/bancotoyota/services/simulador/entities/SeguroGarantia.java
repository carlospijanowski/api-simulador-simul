package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SeguroGarantia {
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

    @Schema(name = "data-inicio-garantia-estendida", description = "Data de início.", example = "20180126")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-inicio-garantia-estendida")
    private LocalDate dataInicioGarantiaEstendida;
}
