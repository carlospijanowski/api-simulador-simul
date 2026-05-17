package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ItemSeguroGarantia {
    @Schema(name = "id", description = "Identificador do seguro", example = "1")
    private String id;

    @Schema(name = "importancia-segurada", description = "Valor máximo segurado pelo seguro", example = "10000")
    @JsonProperty("importancia-segurada")
    private String importanciaSegurada;

    @Schema(name = "descricao", description = "Nome do seguro ofertado", example = "Assurant - Extra 1")
    private String descricao;

    @Schema(name = "valor-premio-liquido", description = "Valor do prêmio líquido", example = "1460.0")
    @JsonProperty("valor-premio-liquido")
    private BigDecimal valorPremioLiquido;

    @Schema(name = "premio-net", description = "Valor do prêmio net", example = "1860.0")
    @JsonProperty("premio-net")
    private BigDecimal valorPremioNet;

    @Schema(name = "premio-total", description = "Valor do prêmio total do seguro", example = "2108.49")
    @JsonProperty("premio-total")
    private BigDecimal valorPremioTotal;

    @Schema(name = "prazo-seguro", description = "Prazo de vigência doseguro", example = "24")
    @JsonProperty("prazo-seguro")
    private Integer prazoSeguro;

    @Schema(name = "comissao", description = "Percentual de comissão do seguro", example = "9.0")
    private Double comissao;

    @Schema(name = "iof", description = "Percentual do IOF do seguro", example = "4.0")
    private Double iof;

    @Schema(name = "inicio-vigencia", description = "Data inicial da vigência do seguro", example = "20/03/2025")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @JsonProperty("inicio-vigencia")
    private LocalDate inicioVigencia;

    @Schema(name = "fim-vigencia", description = "Data final da vigência do seguro", example = "07/03/2026")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    @JsonProperty("fim-vigencia")
    private LocalDate fimVigencia;

    @Schema(name = "categoria", description = "Categoria do seguro", example = "Toyota Leves Nacional")
    @JsonProperty("categoria")
    private String categoria;

    @Schema(name = "itens-ativos", description = "Indicador se o seguro está ativo", example = "true")
    @JsonProperty("itens-ativos")
    private Boolean itensAtivos;

    @Schema(name = "tipo-contratacao", description = "Tipo da contratação do seguro", example = "1")
    @JsonProperty("tipo-contratacao")
    private String tipoContratacao;

    @Schema(name = "tipo-plano", description = "Tipo do seguro", example = "EXTRA")
    @JsonProperty("tipo-plano")
    private String tipo;
}
