package br.com.bancotoyota.services.simulador.beans.geolocalizacao;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = {"itemFinanciavelServicoGeolocalizacao"})
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DadosCobrancaServicoGeolocalizacao {
    @Schema(name = "qtd-meses", description = "Quantidade de meses do seguro auto da seguradora credenciada.", example = "60")
    @JsonProperty("qtd-meses")
    int qtdMeses = 0;

    @Schema(name = "qtd-meses-subsidio-seguradora", description = "Quantidade de meses do subsidio da seguradora.", example = "60")
    @JsonProperty("qtd-meses-subsidio-seguradora")
    int qtdMesesSubsidioSeguradora = 0;

    @Schema(name = "qtd-meses-subsidio-tdb", description = "Quantidade de meses do subsidio da TDB.", example = "60")
    @JsonProperty("qtd-meses-subsidio-tdb")
    int qtdMesesSubsidioTDB = 0;

    @Schema(name = "qtd-meses-subsidio-btb", description = "Quantidade de meses do subsidio da BTB.", example = "60")
    @JsonProperty("qtd-meses-subsidio-btb")
    int qtdMesesSubsidioBTB = 0;

    @Schema(name = "qtd-meses-subsidio-cliente", description = "Quantidade de meses do subsidio do cliente.", example = "60")
    @JsonProperty("qtd-meses-subsidio-cliente")
    int qtdMesesCobradoDoCliente = 0;

    @Schema(name = "valor-cobrado-cliente", description = "Valor do serviço para o cliente.", example = "1230.23")
    @JsonProperty("valor-cobrado-cliente")
    BigDecimal valorCobradoCliente = new BigDecimal(BigInteger.ZERO);

    @Schema(name = "valor-repasse-btb", description = "Valor do serviço a ser repassado para o BTB", example = "60")
    @JsonProperty("valor-repasse-btb")
    BigDecimal valorRepasseBTB = new BigDecimal(BigInteger.ZERO);

    @JsonIgnore
    ItemFinanciavel itemFinanciavelServicoGeolocalizacao;
}