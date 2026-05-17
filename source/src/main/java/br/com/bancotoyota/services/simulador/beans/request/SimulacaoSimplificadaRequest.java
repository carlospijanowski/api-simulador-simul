package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.entities.SituacaoVeiculo;
import br.com.bancotoyota.services.simulador.entities.TipoPessoa;
import br.com.bancotoyota.services.simulador.entities.UfEmplacamento;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public abstract class SimulacaoSimplificadaRequest {

    @Schema(name = "valor-bem", description = "Valor do veículo", example = "90000")
	@ApiModelProperty(required=true)
    @JsonProperty("valor-bem")
    @NotNull
    private BigDecimal valorBem;


    @Schema(name = "valor-seguro-franquia", description = "Valor do seguro franquia", example = "1232.23")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-seguro-franquia")
    private BigDecimal valorSeguroFranquia;

    @Schema(name = "valor-entrada", description = "Valor de entrada no financiamento", example = "27000")
	@ApiModelProperty(required=true)
    @JsonProperty("valor-entrada")
    @NotNull
    private BigDecimal valorEntrada;

    @Schema(name = "uf-emplacamento", description = "UF do endereço de emplacamento do bem", example = "SP")
    @JsonProperty("uf-emplacamento")
    private UfEmplacamento ufEmplacamento;

    @Schema(name = "cnpj-origem-negocio", description = "CNPJ da loja", example = "91919940439101")
	@ApiModelProperty(required=true)
    @JsonProperty("cnpj-origem-negocio")
    @NotNull
    private String cnpjOrigemNegocio;

    @Schema(name = "cpf-cnpj-cliente", description = "CPF ou CPNJ do cliente.", example = "91919940439101")
    @JsonProperty("cpf-cnpj-cliente")
    private String cpfCnpjCliente;

    @Schema(name = "marca-id", description = "Identificador da marco do veículo", example = "002")
    @ApiModelProperty(required=true)
    @JsonProperty("marca-id")
    @NotNull
    private String marcaId;

    /*
     * Parametrso do seguro garantia
     * */
    @JsonProperty("parametros-seguro-garantia")
    @Valid
    private ParametrosSeguroMecanica parametrosSeguroMecanica;

    @Schema(name = "modelo-id", description = "Identificador da modelo do veículo", example = "002112")
    @ApiModelProperty(required=true)
    @JsonProperty("modelo-id")
    @NotNull
    private String modeloId;

    @Schema(name = "ano-modelo", description = "Ano do modelo do veículo", example = "2019")
    @JsonProperty("ano-modelo")
    private Integer anoModelo;

    @Schema(name = "data-primeiro-vencimento", description = "Data primeiro vencimento do financiamento", example = "20250201")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-primeiro-vencimento")
    private LocalDate dataPrimeiroVencimento;

    @Schema(name = "taxa-subsidio", description = "Taxa do subsidio para financiamento", example = "1.02")
    @JsonProperty("taxa-subsidio")
    private BigDecimal taxaSubsidio;

    @Schema(name = "seguro-auto", description = "Valor do seguro auto do bem a ser financiado",example = "3510.12" )
    @JsonProperty("seguro-auto")
    private BigDecimal seguroAuto;

    @JsonProperty("itens-financiaveis")
    @Valid
    private List<ItemFinanciavel> itens;

    @Schema(name = "svp-removido", description = "Indica se removeu o seguro vida prestamista.", example = "false")
    @JsonProperty("svp-removido")
    private Boolean svpRemovido;

    @Schema(name = "spf-removido", description = "Indica se removeu o seguro proteção financeira.", example = "false")
    @JsonProperty("spf-removido")
    private Boolean spfRemovido;

    @Schema(name = "situacao-veiculo", description = "Estado de conservação do bem.", example = "0km")
    @ApiModelProperty(notes = "0km ou usado. Quando não enviado, considera 0km")
    @JsonProperty("situacao-veiculo")
    private SituacaoVeiculo situacaoVeiculo;

    @Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação", example ="OBO" )
    @ApiModelProperty(notes = "ABRADIT, INSTITUCIONAL, LANDING_PAGE, TDB, OBO")
    @JsonProperty("origem-solicitacao")
    private String origemSolicitacao;

    @Schema(name = "session-id", description = "Identificador da sessão do direct", example ="333133-35464-343uifadbe4d" )
    @JsonProperty("session-id")
    private String sessionId;

    @Schema(name = "tipo-pessoa", description = "Tipo de pessoa", example = "pessoa-fisica")
    @ApiModelProperty(notes = "pessoa-fisica OU pessoa-juridica. Quando não enviado, considera pessoa-fisica")
    @JsonProperty("tipo-pessoa")
    private TipoPessoa tipoPessoa;

    @Schema(name = "codigo-retorno", description = "Código da simulação (Código de Retorno)", example = "150")
    @ApiModelProperty(notes = "Retorno parametrizado pela loja.")
    @JsonProperty("codigo-retorno")
    private String codigoRetorno;    
    
}
