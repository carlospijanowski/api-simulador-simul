package br.com.bancotoyota.services.simulador.beans.geolocalizacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParametrizacaoCobrancaGeo {
    @Schema(name = "prazo-inicio", description = "Prazo inicial do serviço", example = "1")
    @JsonProperty("prazo-inicio")
    private Integer inicio = 0;

    @Schema(name = "prazo-fim", description = "Prazo inicial do serviço", example = "12")
    @JsonProperty("prazo-fim")
    private Integer fim = 0;

    @Schema(name = "seguro-contrato", description = "Indica se tem seguro contratado.", example = "false")
    @JsonProperty("seguro-contrato")
    private Boolean contratou = false;

    @Schema(name = "periodo-contrato-seguro", description = "Indica se a parametrização do seguro é pelo período do contrato ", example = "false")
    @JsonProperty("periodo-contrato-seguro")
    private Boolean periodoDoContrato = false;

    @Schema(name = "qtd-meses-seguro", description = "Quantidade de meses do seguro.", example = "12")
    @JsonProperty("qtd-meses-seguro")
    private Integer qtdMeses = 0;

    @Schema(name = "cnpj-seguradora", description = "CPNJ da seguradora.", example = "33016221000107")
    @JsonProperty("cnpj-seguradora")
    private String cnpjSeguradora = null;

    @Schema(name = "subsidio-tdb", description = "Quantidade de meses do subsidio da TDB.", example = "60")
    @JsonProperty("subsidio-tdb")
    private Integer tdb;

    @Schema(name = "subsidio-btb", description = "Quantidade de meses do subsidio da BTB.", example = "60")
    @JsonProperty("subsidio-btb")
    private Integer btb;

    @Schema(name = "subsidio-seguradora", description = "Quantidade de meses do subsidio da seguradora.", example = "60")
    @JsonProperty("subsidio-seguradora")
    private Integer seguradora;

    @Schema(name = "qtd-meses", description = "Quantidade de meses que serão cobrados para o cliente.", example = "60")
    @JsonProperty("qtd-meses-cobrado-cliente")
    private Integer qtdMesesCobradoCliente;
}