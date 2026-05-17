package br.com.bancotoyota.services.simulador.entities;

import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosSeguroGeoLocalizacao;
import br.com.bancotoyota.services.simulador.beans.response.ParcelasIntermediarias;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeDeserializer;
import br.com.bancotoyota.services.simulador.utils.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Plano {
    @Schema(name = "codigo-modalidade", description = "Codigo da modalidade do veiculo", example = "ciclo-toyota")
    @JsonProperty("codigo-modalidade")
    private String codigoModalidade;

    @Schema(name = "codigo-plano", description = "Código do plano.", example = "3294")
    @JsonProperty("codigo-plano")
    private String codigoPlano;

    @Schema(name = "codigo-base-plano", description = "Código da base plano (BA).", example = "3180")
    @JsonProperty("codigo-base-plano")
    private String codigoBasePlano;

    @Schema(name = "descricao", description = "Nome do plano.", example = "CICLO SNOVOS 122")
    private String descricao;

    @Schema(name = "ano-modelo-inicio", description = "Ano inicial do modelo do veiculo", example = "2018")
    @JsonProperty("ano-modelo-inicio")
    private Integer anoModeloInicio;

    @Schema(name = "ano-modelo-final", description = "Ano final do modelo do veiculo", example = "2018")
    @JsonProperty("ano-modelo-final")
    private Integer anoModeloFinal;

    @Schema(name = "data-inicial-vigencia", description = "Data inicial de vigência do plano.", example = "2020-07-30 00:00:00.000")
    @JsonProperty("data-inicial-vigencia")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime dataInicialVigencia;

    @JsonProperty("uso-plano")
    private UsoPlanoObject usoPlano;

    @Schema(name = "parcela-minima-entrada", description = "Percentual minima de entrada do plano", example = "30")
    @JsonProperty("parcela-minima-entrada")
    private BigDecimal parcelaMinimaEntrada;

    @Schema(name = "parcela-maxima-entrada", description = "Percentual maximo de entrada do plano", example = "100")
    @JsonProperty("parcela-maxima-entrada")
    private BigDecimal parcelaMaximaEntrada;

    @Schema(name = "parcela-minima-balao", description = "Percentual mínimo do residual", example = "20")
    @JsonProperty("parcela-minima-balao")
    private BigDecimal parcelaMinimaBalao;

    @Schema(name = "parcela-maxima-balao", description = "Percentual maximo do residual", example = "50")
    @JsonProperty("parcela-maxima-balao")
    private BigDecimal parcelaMaximaBalao;

    @Schema(name = "quantidade-minima-carencia", description = "Dias minimo de carencia do plano", example = "20")
    @JsonProperty("quantidade-minima-carencia")
    private Integer quantidadeMinimaCarencia;

    @Schema(name = "quantidade-maxima-carencia", description = "Dias maxima de carencia do plano", example = "210")
    @JsonProperty("quantidade-maxima-carencia")
    private Integer quantidadeMaximaCarencia;

    @Schema(name = "tipo-plano", description = "Tipo plano.", example = "CDC-TCM")
    @JsonProperty("tipo-plano")
    private String tipoPlano;

    @Schema(name = "plano-subsidiado", description = "Indicada se o plano tem subsidio", example = "true")
    @JsonProperty("plano-subsidiado")
    private Boolean planoSubsidiado;

    @Schema(name = "tipo-subsidio", description = "Tipo de subsidio do plano", example = "V")
    @JsonProperty("tipo-subsidio")
    private String tipoSubsidio;

    @Schema(name = "primeiro-vencimento-fixo", description = "Indica se o plano possui o primeiro vencimento da parcela fixo", example = "true")
    @JsonProperty("primeiro-vencimento-fixo")
    private Boolean primeiroVencimentoFixo;

    @Schema(name = "indicador-faixa-ano", description = "Codigo de identificação da faixa ano do veículo", example = "AI_E_0KM")
    @JsonProperty("indicador-faixa-ano")
    private String indicadorFaixaAno;

    @Schema(name = "prazo-validade", description = "Codigo de identificação da faixa ano do veiculo", example = "5")
    @JsonProperty("prazo-validade")
    private Integer prazoValidade;

    @Schema(name = "id-modalidade", description = "Identificador da modalidade do veiculo", example = "12")
    @JsonProperty("id-modalidade")
    private String idModalidade;

    @Schema(name = "isenta-iof", description = "Indica se o IOF está isento no financiamento", example = "N")
    @JsonProperty("isenta-iof")
    private String isentaIof;

    @Schema(name = "periodicidade", description = "Período do plano", example = "MENSAL")
    private String periodicidade;

    @Schema(name = "periodicidade-numero", description = "Numero da períodicidade da parcela", example = "1")
    @JsonProperty("periodicidade-numero")
    private Integer periodicidadeNumero;

    @Schema(name = "permite-balao", description = "Indica se o financiamento permite parcelas intermediarias", example = "false")
    @JsonProperty("permite-balao")
    private Boolean permiteBalao;

    @Schema(name = "balao-ultima", description = "Indica se o financimaneto exige residual na ultima parcela", example = "false")
    @JsonProperty("balao-ultima")
    private Boolean balaoUltima;

    @JsonProperty("controle-balao")
    private EnumControleBalao controleBalao;

    @JsonProperty("parcelas-intermediarias")
    private ParcelasIntermediarias parcelasIntermediarias;

    @Schema(name = "habilita-geolocalizacao", description = "Indica se está habilitado o serviço de geolocalização", example = "false")
    @JsonProperty("habilita-geolocalizacao")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean habilitaGeoLocalizacao = false;

    @JsonProperty("parametros-geolocalizacao")
    private DadosSeguroGeoLocalizacao parametrosGeoLocalizacao;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class UsoPlanoObject {

        @Schema(name = "uso-plano", description = "Uso plano.")
        @JsonProperty("uso-plano")
        private List<String> usoPlano;

        @Schema(name = "codigo-marca", description = "Codigo da marca do veiculo")
        @JsonProperty("codigo-marca")
        private List<String> codigoMarca;
    }
}
