package br.com.bancotoyota.services.simulador.beans.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ParcelaIntermediaria {

    @Schema(name = "valor", description = "Valor da parcela.")
	@ApiModelProperty(required=true)
    @NotNull
    private BigDecimal valor;

    @JsonIgnore
    private long numeroParcela;

    /**
     * Poderá ser nulo no caso de uma parcela residual caso seja a única intermediária na lista
     */
    @Schema(name = "vencimento", description = "Data de vencimento de cada parcela.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate vencimento;
}
