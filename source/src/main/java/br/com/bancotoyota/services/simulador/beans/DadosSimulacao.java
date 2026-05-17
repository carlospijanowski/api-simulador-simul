package br.com.bancotoyota.services.simulador.beans;

import br.com.bancotoyota.services.simulador.beans.response.CalculoSeguroMecanicaResponse;
import br.com.bancotoyota.services.simulador.beans.response.TipoDeSimulacao;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.entities.TipoCalculo;
import br.com.bancotoyota.services.simulador.entities.TipoFinanciamento;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Luis santos
 * Bean com os dados para a simulação
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DadosSimulacao implements Serializable {

	private static final long serialVersionUID = 1L;

	private TipoDeSimulacao idSimulacao;
    private LocalDate dataSimulacao;
    private LocalDate dataPrimeiroVencimento;
    private BigDecimal valorBem;
    private BigDecimal vlrSeguroFranquia;
    private BigDecimal valorEntrada;
    private BigDecimal valorSeguroPrestamista;
    private BigDecimal valorSeguroGarantia;

    @JsonProperty("dados-seguro-mecanica")
    private CalculoSeguroMecanicaResponse dadosSeguroMecanica;
    /**
     * É usado para determinar se precisamos remover o seguro e rodar nova simulação
     */
    private CustoPercentualDoSeguroPrestamista custoPercentualDoSeguroPrestamista;

    private BigDecimal valorRegistroContrato;
    private BigDecimal valorCestaServico;
    private BigDecimal valorTC;

    /**
     * Taxa da operação na classe DadosSimulacaoPlanilha em %
     */
    private BigDecimal valorTaxaSubsidio;

    /**
     * No caso de cálculo de subsídio é necessário usar o código de retorno "S". Na chamada para a planilha é passado
     * zero, no entanto isso faz com que o campo valorTaxaSubsidio seja passado na posição correta.
     */
    private String codigoRetorno;

    private Integer periodicidade;
    private TipoFinanciamento tipoFinanciamento;

    /**
     * Controla a forma de funcionamento da calculadora: se for TAXA faz o cálculo normalmente, se for SUBSIDIO faz
     * uma busca binária para achar a taxa correspondente ao valor do subsídio.
     */
    private TipoCalculo tipoCalculo;

    /**
     * Quando o tipoCalculo é TAXA o campo valorSubsidio não é usado, mas o valor calculado é retornado no objeto
     * ResultadoCalculadoEx.
     * É usado quanto o tipoCalculo é SUBSIDIO. Nesse caso a taxa equivalente é armazenada no objeto DadosSimulacaoPlanilha
     * usado na chamada.
     */
    private BigDecimal valorSubsidio;

    private boolean temIsencaoIOF;
    private BigDecimal taxaIOFAA;
    private BigDecimal taxaIOFAD;
    private Integer baseCalculoIOF;
    private List<ParcelaBalao> parcelasBalao;
    private Prazo prazo;
    private String tipoPessoa;
    private List<ItemFinanciavel> itens;
    private Boolean usuarioInformouDataPrimeiroVencimento;
    private BigDecimal taxaBanco;

    public BigDecimal getValorAcessorios() {
        if (itens == null) {
            return null;
        } else {
            return itens.stream().map(ItemFinanciavel::getValor).reduce(BigDecimal.ZERO,BigDecimal::add);
        }
    }
}
