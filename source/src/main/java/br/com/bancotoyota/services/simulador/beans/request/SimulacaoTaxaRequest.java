package br.com.bancotoyota.services.simulador.beans.request;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.utils.NumberUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class SimulacaoTaxaRequest {

	@Schema(name = "valor-taxa-banco", description = "Valor da taxa de do banco.", example = "1.35")
	@JsonProperty("taxa-banco")
	@NotNull
	private BigDecimal taxaBanco;
	
	private SubsidioRequest subsidio;

	@Schema(name = "codigo-retorno", description = "Código da simulação (Código de Retorno)", example = "S")
	@JsonProperty("codigo-retorno")
	@NotBlank
	private String codigoRetorno;

	@Schema(name = "prazo", description = "Prazo do financiamento", example = "32")
	@NotNull
	private Integer prazo;

	@Schema(name = "data-inicio", description = "Data do inicio do financiamento", example = "20180126")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")	
	@JsonProperty("data-inicio")
	@NotNull
	private LocalDate dataInicio;

	@Schema(name = "data-primeiro-vencimento", description = "Data primeiro vencimento do financiamento", example = "20241010")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd")	
	@JsonProperty("data-primeiro-vencimento")
	@NotNull
	private LocalDate dataPrimeiroVencimento;
	
	@JsonProperty("informa-primeiro-vencimento")
	@NotNull
	private Boolean informaPrimeiroVencimento;

	@Schema(name = "valor-bem", description = "Valor do veículo", example = "90000")
	@JsonProperty("valor-bem")
	@NotNull
	private BigDecimal valorBem;

	@Schema(name = "valor-tarifa", description = "Valor da tarifa de cadastro", example = "950")
	@JsonProperty("valor-tarifa")
	@NotNull
	private BigDecimal valorTarifa;

	@Schema(name = "valor-uf-emplacamento", description = "Valor do registro de contrato para UF de emplacamento escolhida.")
	@JsonProperty("valor-uf-emplacamento")
	@NotNull
	private BigDecimal valorUfEmplacamento;

	@Schema(name = "valor-acessorios", description = "Valor dos acessórios inclusos no financiamento", example = "5000")
	@JsonProperty("valor-acessorios")
	@NotNull
	private BigDecimal valorAcessorios;

	@Schema(name = "valor-seguro-prestamista", description = "Valor do seguro prestamista", example = "4000")
	@JsonProperty("valor-seguro-prestamista")
	@NotNull
	private BigDecimal valorSeguroPrestamista;

	@Schema(name = "valor-seguro-auto", description = "Valor do seguro bem a ser financiado", example = "11000")
	@JsonProperty("valor-seguro-auto")
	@NotNull
	private BigDecimal valorSeguroAuto;

	@Schema(name = "valor-seguro-franquia", description = "Valor do seguro franquia", example = "1232.23")
	@JsonProperty("valor-seguro-franquia")
	private BigDecimal valorSeguroFranquia;

	@Schema(name = "valor-entrada", description = "Valor de entrada no financiamento", example = "27000")
	@JsonProperty("valor-entrada")
	@NotNull
	private BigDecimal valorEntrada;

	@Schema(name = "aliquota-iof", description = "Percentual do IOF no financiamento", example = "3")
	@JsonProperty("aliquota-iof")
	@NotNull
	private BigDecimal aliquitaIof;

	@Schema(name = "aliquota-iof-adicional", description = "Percentual do IOF adicional do financiamento", example = ":0.381448")
	@JsonProperty("aliquota-iof-adicional")
	@NotNull
	private BigDecimal aliquitaIofAdicional;

	@Schema(name = "periodicidade", description = "Período do plano", example = "1")
	private Integer periodicidade;
	
	@JsonProperty("parcelas-balao")
	private List<ParcelaBalaoRequest> parcelasBalao;

	@Schema(name = "id-taxa", description = "Id da taxa no sistema RBP Configurador", example = "123")
	@JsonProperty("id-taxa")
	private Integer idTaxa;

	@Schema(name = "origem-solicitacao", description = "Sistema de origem da solicitação", example ="autbank" )
	@JsonProperty("origem-solicitacao")
    private String origemSolicitacao;

	@JsonProperty("valor-seguro-garantia")
	@NotNull
	private BigDecimal valorSeguroGarantia;


    public BigDecimal calcularValorTotalFinanciado () {
        // Cálculo do valor do seguro: taxa do seguro * valor financiado onde esse último é:
        // (valor do bem - entrada + cesta + registro de contrato + items + emplacamento)
        return getValorBem().subtract(getValorEntrada()).
                add(getValorTarifa() != null ? getValorTarifa() : BigDecimal.ZERO).
                add(getValorUfEmplacamento() != null ? getValorUfEmplacamento() : BigDecimal.ZERO).
                add(getValorAcessorios() != null ? getValorAcessorios() : BigDecimal.ZERO).
				add(getValorSeguroPrestamista() != null ? getValorSeguroPrestamista() : BigDecimal.ZERO).
				add(getValorSeguroFranquia() != null ? getValorSeguroFranquia() : BigDecimal.ZERO).
				add(getValorSeguroGarantia() != null ? getValorSeguroGarantia() : BigDecimal.ZERO).
                add(getValorSeguroAuto() != null ? getValorSeguroAuto() : BigDecimal.ZERO);
    }	
    
    public BigDecimal calcularValorIofAdicional () {
    	//(AliquotaIOFAdicional/100) * valorTotalFinanciado
    	//return duasCasasDecimais(getAliquitaIofAdicional().divide(NumberUtils.CEM).multiply(calcularValorTotalFinanciado()));
    	
    	//IOF Adicional financiado
    	//(valorTotalFinanciado * ((AliquotaIOFAdicional/100)/(1-(AliquotaIOFAdicional/100))))
    	BigDecimal valorTotalFinanciado =calcularValorTotalFinanciado();
    	BigDecimal porcentagemIOFAdicional = getAliquitaIofAdicional().divide(NumberUtils.CEM);
    	BigDecimal parteDeBaixo = BigDecimal.ONE.subtract(getAliquitaIofAdicional().divide(NumberUtils.CEM));
    	BigDecimal divisao = (porcentagemIOFAdicional).divide(parteDeBaixo,20,RoundingMode.HALF_UP);
    	
    	return duasCasasDecimais(valorTotalFinanciado.multiply(
    			(porcentagemIOFAdicional)
    			.divide(parteDeBaixo,20,RoundingMode.HALF_UP
    					)));
    	
    }
    
    private BigDecimal duasCasasDecimais(BigDecimal valor) {
        if (valor != null) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }    
	
}
