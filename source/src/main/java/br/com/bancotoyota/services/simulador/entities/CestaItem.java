package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CestaItem {

    @Schema(name = "codigo-despesa", description = "Código do item da cesta de serviço", example = "1")
    @JsonProperty("codigo-despesa")
    private Integer codigoDespesa;

    @Schema(name = "valor-unitario", description = "Valor unitário do item da cesta de serviço", example = "950.00")
    @JsonProperty("valor-unitario")
    private BigDecimal valorUnitario;

    @Schema(name = "quantidade", description = "Quantidade de itens da cesta de serviço", example = "1")
    private Integer quantidade;

    @Schema(name = "descricao", description = "Nome do item da cesta de serviço", example = "CONFECCAO DE CADASTRO - PF")
    private String descricao;

    @Schema(name = "isenta-cliente-ativo", description = "Indica se a isenta o item para Cliente Ativo", example = "true")
    @JsonProperty("isenta-cliente-ativo")
    private Boolean isentaClienteAtivo;
}
