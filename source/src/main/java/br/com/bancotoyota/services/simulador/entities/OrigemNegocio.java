package br.com.bancotoyota.services.simulador.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrigemNegocio {

    @Schema(name = "codigo", description = "Código da loja", example = "175")
    private String codigo;

    @Schema(name = "cnpj", description = "CNPJ da loja", example = "33016221000107")
    private String cnpj;

    @Schema(name = "cidade", description = "Cidade", example = "CHAPECO")
    private String cidade;

    @Schema(name = "estado", description = "Estado", example = "SC")
    private String estado;

    @Schema(name = "status", description = "Status da loja (A-ATIVO/I-INATIVO)", example = "A")
    private String status;

    @Schema(name = "cnpj-favorecido", description = "CNPJ da loja", example = "33016221000107")
    @JsonProperty("cnpj-favorecido")
    private String cnpjFavorecido;

    @Schema(name = "nome-favorecido", description = "Nome da loja", example = "DIAMANTINA VEICULOS LTDA")
    @JsonProperty("nome-favorecido")
    private String nomeFavorecido;

    @Schema(name = "cnpj-matriz", description = "CNPJ da loja matriz", example = "33016221000107")
    @JsonProperty("cnpj-matriz")
    private String cnpjMatriz;

    @Schema(name = "codigo-regiao", description = "Código da região", example = "BRASIL")
    @JsonProperty("codigo-regiao")
    private String codigoRegiao;

    @Schema(name = "usa-markup", description = "Indica se a loja possui markup no código da simulação", example = "false")
    @JsonProperty("usa-markup")
    private Boolean usaMarkup;

    @Schema(name = "spf-disponivel", description = "Indica se está disponivel o seguro proteção financeira.", example = "false")
    @JsonProperty("spf-disponivel")
    private Boolean spfDisponivel;

    @Schema(name = "svp-disponivel", description = "Indica se está disponivel o seguro vida prestamista.", example = "false")
    @JsonProperty("svp-disponivel")
    private Boolean svpDisponivel;

    @Schema(name = "svp-se-spf-removido", description = "Indica se o SVP pode ser adicionado automaticamente caso o SPF seja removido.", example = "false")
    @JsonProperty("svp-se-spf-removido")
    private Boolean svpSeSpfRemovido;

    @Schema(name = "trava-svp", description = "Indica se a loja não pode remover o SVP", example = "false")
    @JsonProperty("trava-svp")
    private Boolean travaSvp;

    @Schema(name = "trava-spf", description = "Indica se a loja não pode remover o SPF", example = "false")
    @JsonProperty("trava-spf")
    private Boolean travaSpf;
}
