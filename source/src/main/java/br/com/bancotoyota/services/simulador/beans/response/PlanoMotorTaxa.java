package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.geolocalizacao.DadosSeguroGeoLocalizacao;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class PlanoMotorTaxa {


    private ObjectId id;

    @NotNull
    @ApiModelProperty(required=true)
    @JsonProperty("plano-id")
    private Integer planoId;

    @JsonProperty("id-configurador")
    private Integer idConfigurador;

    private Integer versao;

    @NotNull
    @JsonProperty("status-versionamento")
    private String statusVersionamento;

    private String nome;

    private String descricao;

    @JsonProperty("ordem-priorizacao")
    private Integer ordemPriorizacao;

    @NotNull
    @ApiModelProperty(required=true)
    private TipoPlano tipo;

    @NotNull
    @ApiModelProperty(required=true)
    @JsonProperty("tipo-pessoa")
    private TipoPessoa tipoPessoa;

    @NotNull
    @ApiModelProperty(required=true)
    @JsonProperty("data-inicial-vigencia")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate dataInicialVigencia;

    @JsonProperty("data-final-vigencia")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")
    private LocalDate dataFinalVigencia;

    @NotNull
    @ApiModelProperty(required=true)
    @JsonProperty("prazo-validade")
    private Integer prazoValidade;

    @JsonProperty("quantidade-minima-carencia")
    private Integer quantidadeMinimaCarencia;

    @JsonProperty("quantidade-maxima-carencia")
    private Integer quantidadeMaximaCarencia;

    @NotNull
    @JsonProperty("marcas-modelos")
    private TipoFiltro marcasModelos;

    @JsonProperty("lista-marcas-modelos")
    private List<MarcaModelo> listaMarcasModelos;

    @NotNull
    @ApiModelProperty(required=true)
    private Taxa taxa;

    @NotNull
    @JsonProperty("tipo-loja")
    private TipoFiltro tipoLoja;

    //TOYOTA,INDEPENDENTE,COLIGADAS
    @JsonProperty("lista-tipos-lojas")
    private List<String> listaTiposLojas;

    @NotNull
    @JsonProperty("plano-tipo-especial")
    private TipoFiltro planoTipoEspecial;

    @JsonProperty("lista-planos-tipo-especial")
    private List<String> ListaPlanosTipoEspecial;

    @NotNull
    @JsonProperty("grupo-lojas")
    private TipoFiltro grupoLojas;

    @JsonProperty("lista-grupo-lojas")
    private List<GrupoLoja> listaGrupoLojas;

    @NotNull
    @JsonProperty("aplicacao")
    private TipoFiltro aplicacao;

    @JsonProperty("lista-aplicacoes")
    private List<Aplicacao> listaAplicacoes;

    @NotNull
    @JsonProperty("cpf-cnpjs")
    private TipoFiltro cpfCnpjs;

    @JsonProperty("lista-cpf-cnpjs")
    private List<String> listaCpfCnpjs;

    @NotNull
    @JsonProperty("periodicidade")
    private TipoFiltro periodicidade;

    @JsonProperty("lista-periodicidades")
    private List<Periodicidade> listaPeriodicidades;

    @NotNull
    @JsonProperty("tipo-veiculo")
    private TipoFiltro tipoVeiculo;

    @JsonProperty("lista-tipos-veiculo")
    private List<TipoVeiculo> listaTiposVeiculo;

    @NotNull
    @JsonProperty("perfil")
    private TipoFiltro perfil;

    @JsonProperty("lista-perfis")
    private List<PerfilUsuario> listaPerfis;

    //@JsonProperty("grupo-carteira")
    //private TipoFiltro grupoCarteira;

    //Recebe os ids das carteiras
    //@JsonProperty("lista-grupo-carteiras")
    //private List<String> listaGrupoCarteiras;

    @JsonProperty("plano-subsidiado")
    private Boolean planoSubsidiado;

    private SubsidioMotorTaxa subsidio;

    @NotNull
    @ApiModelProperty(required=true,notes="Código do Plano no BA")
    @JsonProperty("plano-codigo-ba")
    private Integer planoCodigoBA;

    @NotNull
    @JsonProperty("plano-origem")
    private String planoOrigem;

    @JsonProperty("isenta-iof")
    private Boolean isentaIof;

    @JsonProperty("parcela-minima-entrada")
    private BigDecimal parcelaMinimaEntrada;

    @JsonProperty("parcela-maxima-entrada")
    private BigDecimal parcelaMaximaEntrada;

    @JsonProperty("prazo-minimo")
    private Integer prazoMinimo;

    @JsonProperty("prazo-maximo")
    private Integer prazoMaximo;

    @JsonProperty("dados-externos")
    private DadosExternos dadosExternos;

    @JsonProperty("codigos-retorno")
    private List<CodigoSimulacao> listaCodigosRetorno;

    private Boolean bloqueado;

    @JsonProperty("primeiro-vencimento")
    private PrimeiroVencimento primeiroVencimento;

    @JsonProperty("parcelas-intermediarias")
    private ParcelasIntermediarias parcelasIntermediarias;

    @JsonProperty("habilita-geolocalizacao")
    private Boolean habilitaGeoLocalizacao;

    @JsonProperty("parametros-geolocalizacao")
    private DadosSeguroGeoLocalizacao parametrosGeoLocalizacao;
}