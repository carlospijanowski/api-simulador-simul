package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.TipoDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosCobrancaServicoGeolocalizacao;
import br.com.bancotoyota.services.simulador.entities.Seguro;
import br.com.bancotoyota.services.simulador.entities.Subsidio;
import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import br.com.bancotoyota.simuladorpropostasservices.entidades.simulacao.ResultadoCalculado;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uk.co.jemos.podam.common.PodamExclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * @author Luis Santos
 * Bean com os dados da simulação
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class PrazoResidualResponse implements Serializable {

    @Schema(name = "parcelas", description = "Número da parcela", example = "1")
    @JsonProperty("parcelas")
    private Integer parcelas;

    @Schema(name = "valor-parcela", description = "Valor da parcela", example = "240")
    @JsonProperty("valor-parcela")
    private BigDecimal valorParcela;

    // TODO: Para o CDC acho que não faz sentido ter esse campo na resposta. Mesmo no caso de ser ciclo acho que
    //  não seria necessário uma vez que o valor do residual é passado no request. Teríamos que ver por que
    //  estamos usando esse campo na resposta. Ver com o pessoal do front se eles estão usando.
    @Schema(name = "valor-parcela-residual", description = "Valor da parcela residual", example = "18000")
    @JsonProperty("valor-parcela-residual")
    private BigDecimal valorParcelaResidual;

    @Schema(name = "valor-iof", description = "Valor do IOF.", example = "2137.85")
    @JsonProperty("valor-iof")
    private BigDecimal valorIOF;

    @Schema(name = "valor-cet-anual", description = "Valor do custo efetivo anual do financiamento.", example = "23.02")
    @JsonProperty("valor-cet-anual")
    private BigDecimal taxaCetAnual;

    @Schema(name = "valor-taxa-mes", description = "Valor da taxa mensal do financiamento.", example = " 1.26")
    @JsonProperty("valor-taxa-mes")
    private BigDecimal taxaMensal;

    @Schema(name = "valor-taxa-anual", description = "Valor da taxa anual do financiamento.", example = " 12.26")
    @JsonProperty("valor-taxa-anual")
    private BigDecimal taxaAnual;

    @Schema(name = "valor-total-financiado", description = "Valor do total do financiamento", example = "130000")
    @JsonProperty("valor-total-financiado")
    private BigDecimal valorTotalFinanciado;

    @Schema(name = "valor-seguro-auto-parcela", description = "Valor do seguro auto na parcela", example = "200")
    @JsonProperty("valor-seguro-auto-parcela")
    private BigDecimal valorDoSeguroAutoNaParcela;

    @Schema(name = "valor-seguro-prestamista-parcela", description = "Valor do seguro prestamista na parceka", example = "340")
    @JsonProperty("valor-seguro-prestamista-parcela")
    private BigDecimal valorDoSeguroPrestamistaNaParcela;

    @Schema(name = "tipo-seguro-prestamista", description = "Tipo do seguro prestamista", example = "SPF")
    @JsonProperty("tipo-seguro-prestamista")
    private TipoDeSeguroPrestamista tipoDeSeguroPrestamista;

    @JsonProperty("seguro-franquia")
    private SeguroFranquia seguroFranquia;


    @Schema(name = "valor-seguro-prestamista", description = "Valor do seguro prestamista", example = "4000")
    @JsonProperty("valor-seguro-prestamista")
    private BigDecimal valorDoSeguroPrestamista;

    /**
     * As flags de limite extrapolado não serão usadas pelo front para mostrar mensagens referentes a trocas
     * de tipo de seguro. As mensagens serão mostradas apenas no caso de simulações subsequentes quando houver
     * a troca de seguro prestamista automaticamente, isto é, não por solicitação do usuário.
     * Mas é usado na tela de oferta pós simulação será usado para não oferecer um seguro que teve o limite
     * extrapolado.
     */
    @Schema(name = "svp-limite-extrapolado", description = "Indica se o valor do SVP foi ultrapassado.", example = "false")
    @JsonProperty("svp-limite-extrapolado")
    private Boolean limiteSVPExtrapolado;

    @Schema(name = "spf-limite-extrapolado", description = "Indica se o valor do SPF foi ultrapassado.", example = "false")
    @JsonProperty("spf-limite-extrapolado")
    private Boolean limiteSPFExtrapolado;

    @JsonProperty("itens-financiaveis")
    private List<ItemFinanciavel> itensFinanciaveisNaParcela;

    private ValorSubsidio subsidio;

    @Schema(name = "valor-subsidio-usado", description = "Valor do subsidio calculado pela simulação", example = "345")
    @JsonProperty("valor-subsidio-usado")
    private BigDecimal valorSubsidioUsado;

    @Schema(name = "taxa-subsidio-usada", description = "Taxa de simulação calculada pela simulação", example = "23")
    @JsonProperty("taxa-subsidio-usada")
    private BigDecimal taxaSubsidioUsada;

    @Schema(name = "valor-total-prazo", description = "Valor original antes de alterar proposta", example = "100000")
    @JsonProperty("valor-total-prazo")
    private BigDecimal valorTotalPrazo;

    /**
     * Este campo é retornado apenas no caso do número de prazos ser um, para evitar a necessidade de uma segunda
     * chamada só para obter o fluxo.
     */
    @ApiModelProperty(notes = "é retornado apenas no caso de número de prazos ser um")
    @JsonProperty("fluxo")
    private List<Fluxo> fluxo;

    /**
     * Usado para montar o fluxo no final do processo.
     */
    @JsonIgnore
    @PodamExclude
    ResultadoCalculado resultadoCalculado;

    @Schema(name = "porcentagem-parcela-residual", description = "Porcentagem do residual na parcela", example = "30")
    @JsonProperty("porcentagem-parcela-residual")
    private BigDecimal percentualParcelaResidual;

    @JsonProperty("dados-seguro-prestamista-utilizado")
    private Seguro dadosSeguroPrestamistaUtilizado;

    @JsonProperty("dados-seguro-mecanica")
    private CalculoSeguroMecanicaResponse dadosSeguroMecanica;

    @JsonProperty("dados-ultimo-seguro-prestamista-disponivel")
    private Seguro dadosUltimoSeguroDisponivel;

    @Schema(name = "valor-iof-adicional", description = "Valor do IOF adicional do financiamento.", example = "0.09")
    @JsonProperty("valor-iof-adicional")
    private BigDecimal valorIOFAdicional;

    @Schema(name = "taxa-mensal-completa", description = "Valor da taxa completa sem limite de decimais.", example = "1.35000000")
    @JsonProperty("taxa-mensal-completa")
    private BigDecimal taxaMensalCompleta;

    @Schema(name = "taxa-anual-completa", description = "Valor da total da taxa anual sem limite de casas decimais.", example = "17.45865800")
    @JsonProperty("taxa-anual-completa")
    private BigDecimal taxaAnualCompleta;

    @Schema(name = "taxa-subsidio-usada-completa", description = "Valor da taxa de subsidio sem limite de casas decimais.", example = "1.45865800")
    @JsonProperty("taxa-subsidio-usada-completa")
    private BigDecimal taxaSubsidioUsadaCompleta;

    @Schema(name = "valor-taxa-banco", description = "Valor da taxa de do banco.", example = "1.35")
    @JsonProperty("valor-taxa-banco")
    private BigDecimal valorTaxaBanco;

    @JsonProperty("dados-cobranca-servico-geolocalizacao")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    DadosCobrancaServicoGeolocalizacao dadosCobrancaServicoGeolocalizacao;

    @JsonProperty("fator-rating")
    private FatorRating fatorRating;

    @Schema(name = "valor-subsidio-alterado", description = "Valor do subsidio se for alterado no financiamento", example = "1.35")
    @JsonProperty("valor-subsidio-alterado")
    private boolean valorSubsidioAlterado;

    public PrazoResidualResponse(ResultadoCalculadoParcelaResidual resultado, Subsidio dadosSubsidio) {
        setParcelas(resultado.getPrazo());
        setValorParcela(resultado.getValorParcela().setScale(2, RoundingMode.HALF_UP));
        setValorParcelaResidual(duasCasasDecimais(resultado.getValorParcelaResidual()));
        setValorIOF(resultado.getValorIOF());
        setTaxaCetAnual(resultado.getValorCetAnual());
        setValorTotalFinanciado(resultado.getValorTotalFinanciado());
        setValorTotalPrazo(resultado.getValorTotalPrazo());
        setTaxaMensal(paraPercentagem(resultado.getValorTaxaMes()));
        setTaxaMensalCompleta(paraPercentagemSemTruncamento(resultado.getValorTaxaMes()));
        setTaxaAnual(duasCasasDecimais(resultado.getValorTaxaAnual()));
        setTaxaAnualCompleta(resultado.getValorTaxaAnual());
        setSeguroFranquia(resultado.getSeguroFranquia());
        setTipoDeSeguroPrestamista(resultado.getDadosSeguroPrestamista().getTipo());
        setDadosSeguroPrestamistaUtilizado(resultado.getDadosSeguroPrestamista().getDadosSeguro());
        setDadosUltimoSeguroDisponivel(resultado.getDadosSeguroPrestamista().getUltimoSeguro());
        setLimiteSPFExtrapolado(resultado.getDadosSeguroPrestamista().getLimiteDeSPFExtrapolado());
        setLimiteSVPExtrapolado(resultado.getDadosSeguroPrestamista().getLimiteDeSVPExtrapolado());
        setValorDoSeguroPrestamistaNaParcela(duasCasasDecimais(resultado.getValorDoSeguroPrestamistaNaParcela()));
        setValorDoSeguroPrestamista(duasCasasDecimais(resultado.getValorDoSeguroPrestamista()));
        setValorDoSeguroAutoNaParcela(duasCasasDecimais(resultado.getValorSeguroAutoNaParcela()));
        setItensFinanciaveisNaParcela(resultado.getItensFinanciaveisNaParcela());

        BigDecimal valorSubsidioUsadoFormatado = duasCasasDecimais(resultado.getValorSubsidioUsado());
        setValorSubsidioUsado(valorSubsidioUsadoFormatado);

        BigDecimal valorSubsidio = duasCasasDecimais(resultado.getValorSubsidio());
        if (Objects.nonNull(dadosSubsidio) && Objects.nonNull(dadosSubsidio.getTipoSubsidio())) {
            setSubsidio(dadosSubsidio.fazerDistribuicao(valorSubsidio));
            if (!valorSubsidio.equals(valorSubsidioUsadoFormatado)) {
                setValorSubsidioAlterado(true);
                setValorSubsidioUsado(valorSubsidio);
            }
        }

        setTaxaSubsidioUsada(paraPercentagem(resultado.getTaxaSubsidioUsada()));
        setTaxaSubsidioUsadaCompleta(paraPercentagemSemTruncamento(resultado.getTaxaSubsidioUsada()));

        if (Objects.nonNull(resultado.getDadosCobrancaServicoGeolocalizaocao())) {
            setDadosCobrancaServicoGeolocalizacao(resultado.getDadosCobrancaServicoGeolocalizaocao());
        }

        resultadoCalculado = resultado.getResultadoCalculado();
        setValorIOFAdicional(NumberUtils.toBigDecimal(resultadoCalculado.getValorIOFAdicionalFinanciado()));
        setValorTaxaBanco(resultado.getValorTaxaBanco());
        setDadosSeguroMecanica(resultado.getDadosSeguroMecanica());

        if (resultado.getPercentualParcelaResidual() != null) {
            // ciclo-toyota
            setPercentualParcelaResidual(resultado.getPercentualParcelaResidual().setScale(2, RoundingMode.HALF_UP));
        } else {
            // CDC
            setPercentualParcelaResidual(null);
        }

        fluxo = resultado.getFluxo();
    }

    private BigDecimal paraPercentagem(BigDecimal valor) {
        if (valor != null) {
            return valor.multiply(NumberUtils.CEM).setScale(2, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    private BigDecimal duasCasasDecimais(BigDecimal valor) {
        if (valor != null) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }

    private BigDecimal paraPercentagemSemTruncamento(BigDecimal valor) {
        if (valor != null) {
            return valor.multiply(NumberUtils.CEM);
        } else {
            return null;
        }
    }
}
