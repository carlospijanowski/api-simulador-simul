package br.com.bancotoyota.services.simulador.beans.geolocalizacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;



@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DadosSeguroGeoLocalizacao {
    @Schema(name = "valor-servico-geolocalizacao", description = "Valor do serviço", example = "123.87")
    @JsonProperty("valor-servico-geolocalizacao")
    private BigDecimal valorServicoGeoLocalizacao = BigDecimal.ZERO;

    @Schema(name = "somente-veiculo-com-dispositivo-geo", description = "Indica se permite só veículos com dispositivo de geolocalização", example = "false")
    @JsonProperty("somente-veiculo-com-dispositivo-geo")
    private Boolean permiteDispositivoGeo = false;

    @JsonProperty("parametros")
    private List<ParametrizacaoCobrancaGeo> parametros;
}