package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.ParametrosSeguroMecanica;
import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.response.PercentualBalao;
import br.com.bancotoyota.services.simulador.beans.response.RestricaoPercentualBalao;
import br.com.bancotoyota.services.simulador.beans.seguroautointegrado.SeguroAutoIntegrado;
import br.com.bancotoyota.services.simulador.entities.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import net.logstash.logback.encoder.org.apache.commons.lang3.BooleanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Luis Santos
 * Bean com o request
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Component
@Builder
public class SimulacaoRequest {

    @Schema(name = "modalidade", description = "Modalidade do financiamento.", example = "CDC")
    @JsonProperty("modalidade")
    private Modalidade modalidade;

    @Schema(name = "valor-parcela-desejada", description = "Valor da parcela desejado no financiamento.", example = "1200")
    @JsonProperty("valor-parcela-desejada")
    private BigDecimal valorParcelaDesejada;

    @Schema(name = "data-nascimento", description = "Data de nascimento do beneficiario do bem.", example = "20000322")
    @JsonProperty("data-nascimento")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate dataNascimento;

    /*
    * Valores de carencia da data do primeiro vencimento
    * */
    @ApiModelProperty(required=true)
    @JsonProperty("carencia-do-plano")
    @NotNull
    @Valid
    private Carencia carenciaDoPlano;

    /*
     * Parametrso do seguro garantia mecanica
     * */
    @JsonProperty("parametros-seguro-mecanica")
    @Valid
    private ParametrosSeguroMecanica parametrosSeguroMecanica;


    /*
    * Valor do registro de contrato para UF de emplacamento escolhida
    * */
    @Schema(name = "valor-uf-emplacamento", description = "Valor do registro de contrato para UF de emplacamento escolhida.", example = "123")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-uf-emplacamento")
    @NotNull
    private BigDecimal valorUfEmplacamento;

    /**
     * Data do primeiro vencimento da parcela
     * */
    @Schema(name = "data-primeiro-vencimento", description = "Data do primeiro vencimento da parcela.", example = "20250101")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    @JsonProperty("data-primeiro-vencimento")
    private LocalDate dataPrimeiroVencimento;

    /**
     * Se for true então considera a data do primeiro vencimento como 30 dias após a data do BA.
     * Logo esse campo será removido deixando apenas o campo nDiasAposVencimento.
     */
    @Schema(name = "vencimento-30-dias", description = "Indica a data do primeiro vencimento como 30 dias após a data do BA.", example = "false")
    @JsonProperty("vencimento-30-dias")
    private Boolean trintaDiasAposVencimento;

    @Schema(name = "valor-bem", description = "Valor do carro (valor principal)", example = "12000")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-bem")
    @NotNull
    private BigDecimal valorBem;


    @Schema(name = "valor-entrada", description = "Valor da entrada do financiamento", example = "1234.09")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-entrada")
    @NotNull
    private BigDecimal valorEntrada;

    @Schema(name = "valor-cesta-servicos", description = "Valor total da cesta de serviços", example = "23.0")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-cesta-servicos")
    @NotNull
    private BigDecimal valorCestaServicos;

    @Schema(name = "valor-taxa-cadastro", description = "Valor da tarifa de cadastro", example = "550.00")
    @ApiModelProperty(required=true)
    @JsonProperty("valor-taxa-cadastro")
    @NotNull
    private BigDecimal valorTC;


    @Schema(name = "retorno", description = "Código de Retorno", example = "1")
    @JsonProperty("retorno")
    private String retorno;

    @Schema(name = "plano-id", description = "Codigo do plano", example = "1231")
    @ApiModelProperty(required=true)
    @JsonProperty("plano-id")
    @NotNull
    private Integer planoId;

    @Schema(name = "tipo-pessoa", description = "Tipo de pessoa", example = "pessoa-fisica")
    @ApiModelProperty(required=true)
    @JsonProperty("tipo-pessoa")
    @NotNull
    private String tipoPessoa;

    @Schema(name = "isenta-iof", description = "Indica se o IOF está isento no financiamento", example = "N")
    @JsonProperty("isenta-iof")
    private String isentaIOF;

    /**
     * Número de meses entre as parcelas. Se não for passado assumimos que será mensal.
     */
    @Schema(name = "periodicidade", description = "Número de meses entre as parcelas", example = "1")
    private Integer periodicidade;

    /**
     * Se for true então considera a data do primeiro vencimento como (30*periodiciade) dias após a data do BA.
     */
    @Schema(name = "vencimento-N-dias", description = "Se for true então considera a data do primeiro vencimento como (30*periodiciade) dias após a data do BA.", example = "false")
    @JsonProperty("vencimento-n-dias")
    private Boolean nDiasAposVencimento;

    // Alimentado no SimuladorSimplificadoController.java pelo DTO SimulacaoSimplificada.java
    // Para ser usado em alguns checks ao longo do fluxo da simul.
    @Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação da simulação", example ="OBO" )
    @JsonProperty("origem-solicitacao")
    private String origemSolicitacao;

    public Boolean getNDiasAposVencimento() {
    	return BooleanUtils.isTrue(nDiasAposVencimento) || BooleanUtils.isTrue(trintaDiasAposVencimento);
    }

    @Schema(name = "permite-balao", description = "Indica se o financiamento permite parcelas intermediarias", example = "false")
    @JsonProperty("permite-balao")
    private Boolean permiteBalao;

    @Schema(name = "balao-ultima", description = "Indica se o financimaneto exige residual na ultima parcela", example = "false")
    @JsonProperty("balao-ultima")
    private Boolean balaoUltima;


    @JsonProperty("controle-balao")
    private EnumControleBalao controleBalao;

    @JsonProperty("percentuais-balao")
    private List<PercentualBalao> percentuaisBalao;

    @Schema(name = "intervalo-residual", description = "Distancia entre as parcelas intermediarias", example = "6")
    @JsonProperty("intervalo-residual")
    private Integer intervaloResidual;

    @JsonIgnore
    private Integer quantidadeIntermediarias;

    @Schema(name = "permite-balao-opcional", description = "Indica se as intermediarias são opcionais", example = "false")
    @JsonProperty("permite-balao-opcional")
    private Boolean permiteBalaoOpcional;

    @JsonProperty("restricoes-percentuais-balao")
    private List<RestricaoPercentualBalao> restricoesPercentuaisBalao;

    @JsonIgnore
    public TipoPessoa getTipoPessoaEnum() {
        if (tipoPessoa == null) {
            return null;
        }
        return TipoPessoa.fromValue(tipoPessoa.toUpperCase());
    }

    /**
     * Percentual. Só é usado se o retorno tiver o valor "S".
     */
    @Schema(name = "taxa-subsidio", description = "Taxa do subsidio para financiamento", example = "1.02")
    @JsonProperty("taxa-subsidio")
    private BigDecimal taxaSubsidio;


    @JsonProperty("parametros-seguro-prestamista")
    private ParametrosDeSeguroPrestamista parametrosDeSeguroPrestamista;

    /*
    * Recebe a lista de itens financiaveis para incluir no calculo da simulação
    * */
    @Valid
    @JsonProperty("itens-financiaveis")
    private List<ItemFinanciavel> itens;

    /*
    * Recebe o valor do seguro auto para incluir no calculo da simulação
    * e retornar o valor do seguro referente a cada parcela
    * */
    @Schema(name = "seguro-auto", description = "Valor do seguro auto", example = "2300.12")
    @JsonProperty("seguro-auto")
    private BigDecimal seguroAuto;

    @Schema(name = "valor-seguro-franquia", description = "Valor do seguro franquia.", example = "1200")
    @JsonProperty("valor-seguro-franquia")
    private BigDecimal vlrSeguroFranquia;

    @Schema(name = "parcelas", description = "Quantidade de parcelas disponiveis para financiamento.", example = "[ 12, 24, 36 ]")
    private Integer[] parcelas;

    @Schema(name = "origem-negocio", description = "Dados da loja.")
    private OrigemNegocio origemNegocio;

    @Schema(name = "cpf-proponente", description = "CPF do proponente.", example = "48189660812")
    private String cpfProponente;

    /**
     * Usado caso queira usar uma data de cálculo diferente da data do BA.
     */
    @Schema(name = "data-calculo", description = "Usado caso queira usar uma taxa fixa e diferente da taxa do plano.", example = "20250101")
    @JsonProperty("data-calculo")
    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDate dataCalculo;

    /**
     * Usado caso queira usar uma taxa fixa e diferente da taxa do plano.
     */
    @Schema(name = "taxa-mes", description = "Usado caso queira usar uma taxa fixa e diferente da taxa do plano.", example = "1.32")
    @JsonProperty("taxa-mes")
    private BigDecimal taxaMes;

    /**
     * Indica se foi chamado pela tela do direct ou internamente pelo backend
     */
    @Schema(name = "simulacao-interna", description = "Indica se foi chamado pela tela do direct ou internamente pelo backend", example = "false")
    @JsonProperty("simulacao-interna")
    private Boolean isSimulacaoInterna = false;

    @JsonIgnore
    @JsonProperty("plano-ciclo")
    private Plano planoCiclo;

    @Schema(name = "rating", description = "Identificador do score do cliente.", example = "B")
    @JsonProperty("rating")
    private String rating;

    @JsonProperty("seguro-auto-integrado")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SeguroAutoIntegrado seguroAutoIntegrado;

    @Schema(name = "cnpj-origem-negocio", description = "CNPJ da loja", example = "00827783000181")
    @JsonProperty("cnpj-origem-negocio")
    private String cnpjOrigemNegocio;

    @Schema(name = "percentual-minimo-financiamento", description = "Percentual mínimo de financiamento para utilização nas intermediárias", example = "20.0")
    @JsonProperty("percentual-minimo-financiamento")
    private BigDecimal percentualMinimoFinanciamento;

    public SimulacaoRequest(SimulacaoRequest request) {
        this.dataNascimento = request.getDataNascimento();
        this.carenciaDoPlano = request.getCarenciaDoPlano();
        this.valorUfEmplacamento = request.getValorUfEmplacamento();
        this.dataPrimeiroVencimento = request.getDataPrimeiroVencimento();
        this.trintaDiasAposVencimento = request.getTrintaDiasAposVencimento();
        this.nDiasAposVencimento = request.getNDiasAposVencimento();
        this.valorBem = request.getValorBem();
        this.valorEntrada = request.getValorEntrada();
        this.valorCestaServicos = request.getValorCestaServicos();
        this.valorTC = request.getValorTC();
        this.retorno = request.getRetorno();
        this.planoId = request.getPlanoId();
        this.tipoPessoa = request.getTipoPessoa();
        this.taxaSubsidio = request.getTaxaSubsidio();
        this.parametrosSeguroMecanica = request.getParametrosSeguroMecanica();
        this.parametrosDeSeguroPrestamista = request.getParametrosDeSeguroPrestamista();
        this.itens = request.getItens();
        this.seguroAuto = request.getSeguroAuto();
        this.vlrSeguroFranquia = request.getVlrSeguroFranquia();
        this.isentaIOF = request.getIsentaIOF();
        this.percentuaisBalao = request.getPercentuaisBalao();
        this.origemSolicitacao = request.getOrigemSolicitacao();
        this.rating = request.getRating();
        this.cnpjOrigemNegocio = this.getCnpjOrigemNegocio();
        this.percentualMinimoFinanciamento = request.getPercentualMinimoFinanciamento();
    }

    @JsonIgnore
    public BigDecimal getPercentualEntrada(){
        if(valorEntrada.compareTo(BigDecimal.ZERO) == 0){
            return BigDecimal.ZERO;
        }
        return valorEntrada.divide(valorBem,8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }


    public BigDecimal getVlrSeguroFranquia() {
        return vlrSeguroFranquia == null ? BigDecimal.ZERO : vlrSeguroFranquia;
    }


    public BigDecimal calcularValorBaseParaCalculoDoSeguro() {
        BigDecimal valorDosItensFinanciaveis = BigDecimal.ZERO;
        if (itens != null) {
            valorDosItensFinanciaveis = valorDosItensFinanciaveis.add(itens.stream().map(ItemFinanciavel::getValor).reduce(
                    BigDecimal.ZERO, BigDecimal::add));
        }

        // Cálculo do valor do seguro: taxa do seguro * valor financiado onde esse último é:
        // (valor do bem - entrada + cesta + registro de contrato + items + emplacamento)
        return getValorBem().subtract(getValorEntrada()).
                add(getValorCestaServicos()).add(getValorTC()).add(getValorUfEmplacamento()).add(getVlrSeguroFranquia()).
                add(valorDosItensFinanciaveis).add(getValorSeguroAuto());
    }

    /**
     * @return retorna zero se o seguro auto não tiver sido informado
     */
    @JsonIgnore
    public BigDecimal getValorSeguroAuto() {
        return seguroAuto == null ? BigDecimal.ZERO : seguroAuto;
    }

    public LocalDate getDataPrimeiroVencimento(LocalDate dataCalculo) {
        if (nDiasAposVencimento != null && nDiasAposVencimento) {
            return dataCalculo.plusMonths(getPeriodicidade());
        } else if (trintaDiasAposVencimento != null && trintaDiasAposVencimento) {
            return dataCalculo.plusMonths(1); // esse método trata o caso de cair num dia inválido e seleciona um anterior
        } else {
        	nDiasAposVencimento=false;
            return dataPrimeiroVencimento;
        }
    }

    @JsonIgnore
    public Integer getIdade() {
        if (getDataNascimento() == null) {
            return null;
        }

        // usando a data atual em vez da data do BA pois a cotação do seguro não tem a ver com a data do BA
        return getDataNascimento().until(LocalDate.now()).getYears();
    }

    public Integer getPeriodicidade() {
        return periodicidade == null ? 1 : periodicidade;
    }

    public boolean temPeriodicidade() {
        return periodicidade != null;
    }

}
