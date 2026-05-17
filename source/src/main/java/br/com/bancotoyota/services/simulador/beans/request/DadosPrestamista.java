package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DadosPrestamista {

    /**
     * Pode ser nulo. Se não for nulo então a validação do seguro prestamista deverá usar esse tipo de seguro.
     * Caso não seja possível usar o escolhido então tentaremos as outras opções.
     */
    @JsonProperty("tipo-seguro-prestamista-escolhido")
    private TipoDeSeguroPrestamista tipoDeSeguroPrestamistaEscolhido;

    @JsonProperty("valor-original-total-financiado")
    private BigDecimal valorOriginalTotalFinanciado;


    @JsonProperty("data-nascimento")
    private LocalDate dataNascimento;

    @ApiModelProperty(required=true)
    @JsonProperty("cpf-proponente")
    @NotNull
    private String cpfProponente;

    @JsonProperty("valor-total-financiado")
    private BigDecimal valorTotalFinanciado;

    /**
     * É obrigatório para validar o seguro prestamista.
     */
    @ApiModelProperty(required=true)
    @JsonProperty("valor-iof")
    @NotNull
    private BigDecimal valorIof;

    /**
     * É obrigatório para validar o seguro prestamista.
     * Usado para determinar o tipo de seguro prestamista a ser usado.
     */
    @ApiModelProperty(required=true)
    @JsonProperty("valor-parcela")
    @NotNull
    private BigDecimal valorParcela;
    
    @JsonProperty("permite-spf-com-base-numero-contratos")
    private Boolean permiteSPFComBaseNumeroContratos;
    
    @JsonProperty("permite-svp-com-base-numero-contratos")
    private Boolean permiteSVPComBaseNumeroContratos;

    @JsonProperty("id-seguro-prestamista-escolhido")
    private Integer idSeguroPrestamistaEscolhido;
}
