package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CestaServico {

    @Schema(name = "codigo", description = "Código da cesta de serviços", example = "100")
    private Integer codigo;

    @Schema(name = "descricao", description = "Nome da cesta de serviços", example = "Cesta 10.1 - Pessoa Fisica")
    private String descricao;

    @Schema(name = "valor", description = "Valor total da cesta de serviços", example = "23.0")
    private BigDecimal valor;

    @Schema(name = "cesta-isenta", description = "Indica se a cesta é isenta", example = "true")
    @JsonProperty("cesta-isenta")
    private Boolean cestaIsenta;

    @Schema(name = "cesta-travada-vendedor", description = "Indica se a cesta de serviços é bloqueada de alteração para o vendedor", example = "true")
    @JsonProperty("cesta-travada-vendedor")
    private Boolean cestaTravadaVendedor;

    @Schema(description = "Indica se a cesta é a padrão da loja", example = "true")
    @JsonProperty("cesta-default")
    private Boolean cestaDefault;

    @JsonIgnore
    private List<CestaItem> cestaItems;

    @JsonIgnore
    public BigDecimal getTaxaCadastro() {
        return cestaItems.stream().map(despesa -> Boolean.TRUE.equals(despesa.getIsentaClienteAtivo()) ?
                despesa.getValorUnitario().multiply(new BigDecimal(despesa.getQuantidade())) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonIgnore
    public BigDecimal getValorSemTaxaCadastro() {
        return getValor().subtract(getTaxaCadastro());
    }
}
