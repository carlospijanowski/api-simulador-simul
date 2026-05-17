package br.com.bancotoyota.services.simulador.beans.request;

import br.com.bancotoyota.services.simulador.beans.ItemFinanciavel;
import br.com.bancotoyota.services.simulador.common.Constants;
import br.com.bancotoyota.services.simulador.entities.Carencia;
import br.com.bancotoyota.services.simulador.entities.Modalidade;
import br.com.bancotoyota.services.simulador.entities.Prazo;
import br.com.bancotoyota.services.simulador.services.exceptions.BusinessValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Ints;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Component
public class SimulacaoParcResidualRequest extends SimulacaoRequest {

    /**
     * Se valorParcelaResidual for passado a lista de intermediárias não pode ser passada.
     */
	@Schema(name = "valor-parcela-residual", description = "Valor da parcela residual", example = "18000")
    @JsonProperty("valor-parcela-residual")
    private BigDecimal valorParcelaResidual;

	@Schema(name = "valor-subsidio", description = "Valor total do subsidio", example = "5000")
    @JsonProperty("valor-subsidio")
    private BigDecimal valorSubsidio;

    /**
     * Se lista de intermediarias for passada o valor da parcela residual não pode ser passado.
     * Optamos por suportar as duas formas de especificar a parcela residual: pode ser no campo valorParcelaResidual ou
     * nesta lista. No caso de ser residual a data de vencimento não será informada e deverá haver apenas uma intermediária.
     * Várias intermediárias são suportadas para planos do tipo CDC.
     */
    @JsonProperty("parcelas-intermediarias")
    private List<ParcelaIntermediaria> intermediarias;

    /**
     * Indica se o fluxo deverá ser retornado se houver apenas um prazo.
     */
	@Schema(name = "retornarFluxoPrazoUnico", description = "Indica se o fluxo deverá ser retornado se houver apenas um prazo.", example = "false")
    @JsonIgnore
    private boolean retornarFluxoPrazoUnico = true;

    /**
     * Indica se deverá fazer o cálculo do valor na parcela. Usamos isso na parcela desejada quando precisamos fazer
     * um re-cálculo deviso a mudança do seguro prestamista e em seguida precisaremos fazer novo cálculo para
     * retornar a simulação final. Para a simulação intermediária só precisamos do valor da parcela.
     */
	@Schema(name = "retornarValorNaParcela", description = "Indica se deverá fazer o cálculo do valor na parcela.", example = "false")
    @JsonIgnore
    private boolean retornarValorNaParcela = true;

	@JsonIgnore
	private List<Prazo> prazoDisponivelCiclo;

	/**
	 * Variavel criada para não validar o balao, uma vez que está passando zerada.
	 * Mantendo o comportamento que já existia no IntermediariasController
	 */
	@Schema(name = "intermediariaZerada", description = "Indica que nao devera validar o balão.", example = "false")
	@JsonIgnore
	private boolean intermediariaZerada;

	/**
	 * Lista de percentuais de balão obtidas dos planos para validar se o valor da parcela intermediaria
	 * inserido na simulação respeita os ranges destes balões.
	 */

	@Schema(name = "prazo-desejado", description = "Prazo de financiamento desejado", example = "48")
	@JsonProperty("prazo-desejado")
	private Integer prazoDesejado;

    public SimulacaoParcResidualRequest(SimulacaoRequest request, BigDecimal valorParcelaResidual,
                                        BigDecimal valorSubsidio, Integer... parcelas) {
        super(request);
        this.valorParcelaResidual = valorParcelaResidual;
        this.valorSubsidio = valorSubsidio;
        setParcelas(parcelas);
    }

    public SimulacaoParcResidualRequest(SimulacaoRequest request, List<ParcelaIntermediaria> intermediarias,
                                        BigDecimal valorSubsidio, Integer... parcelas) {
        super(request);
        this.intermediarias = intermediarias;
        this.valorSubsidio = valorSubsidio;
        setParcelas(parcelas);
    }
    
    public SimulacaoParcResidualRequest(SimulacaoRequest request) {
    	super(request);
	}    
    
    public SimulacaoParcResidualRequest(SimulacaoRequest request, Integer... parcelas) {
    	super(request);
    	setParcelas(parcelas);
	}        
    
    public SimulacaoParcResidualRequest(SimulacaoTaxaRequest request) {
    	
    	//Date dataPrimeiroVencimento	
		setDataPrimeiroVencimento(request.getDataPrimeiroVencimento());
		//Boolean informaPrimeiroVencimento	
		setNDiasAposVencimento(!request.getInformaPrimeiroVencimento());
		//SubsidioRequest subsidio
		if (request.getSubsidio() != null) { 
			if (request.getSubsidio().getTaxaSubsidio() == null || request.getSubsidio().getTaxaSubsidio().compareTo(BigDecimal.ZERO) == 0) {
				setValorSubsidio(request.getSubsidio().getValorSubsidio());
			}
			setTaxaSubsidio(request.getSubsidio().getTaxaSubsidio());			
		}
		//Integer codigoRetorno
		//Quando o calculo é feito com subsidio, o codigo-retorno enviado para a calculadora é S
		if ( (this.getTaxaSubsidio() != null && this.getTaxaSubsidio().compareTo(BigDecimal.ZERO) > 0) 
				|| (this.getValorSubsidio() != null && this.getValorSubsidio().compareTo(BigDecimal.ZERO) > 0)) {
			setRetorno("S");
		}else {
			setRetorno(request.getCodigoRetorno());
		}
		//Integer prazo	
		setParcelas(new Integer[] {request.getPrazo()});
		//Date dataInicio	
		setDataCalculo(request.getDataInicio());
		
		//BigDecimal valorBem	
		setValorBem(request.getValorBem());
		
		
		
		//BigDecimal valorTarifa - A autbank passa o valor da tarifa e da cesta de serviços no mesmo campo
		setValorCestaServicos(BigDecimal.ZERO);
		setValorTC(request.getValorTarifa());
		//BigDecimal valorUfEmplacamento	
		setValorUfEmplacamento(request.getValorUfEmplacamento());
		//BigDecimal valorAcessorios	
		if (request.getValorAcessorios() != null && request.getValorAcessorios().compareTo(BigDecimal.ZERO) > 0 ) {
			List<ItemFinanciavel> itens = new ArrayList<>();
			itens.add(new ItemFinanciavel(1, null, request.getValorAcessorios(), null, true, true));
			setItens(itens);
		}
		//BigDecimal valorSeguroAuto
		setSeguroAuto(request.getValorSeguroAuto());
		//BigDecimal valorEntrada
		setValorEntrada(request.getValorEntrada());
		//Integer periodicidade	
		int[] periodicidades = {1,2,3,6,12};
		
		if (request.getPeriodicidade() != null) {
			if (Ints.contains(periodicidades, request.getPeriodicidade())) {
				setPeriodicidade(request.getPeriodicidade());
			}else {
				throw new BusinessValidationException("periodicidade", String.valueOf(request.getPeriodicidade()), "Periodicidade inválida.");
			}
		}
		
		//List<ParcelaBalaoRequest> parcelasBalao
		if (request.getParcelasBalao() != null) {
			List<ParcelaIntermediaria> intermediarias = new ArrayList<>();
			for (ParcelaBalaoRequest p : request.getParcelasBalao()) {
				ParcelaIntermediaria i = new ParcelaIntermediaria();
				i.setValor(p.getValor());
				if (StringUtils.isNotBlank(p.getMesAnoVencimento())) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
					DateTimeFormatter formatterDia = DateTimeFormatter.ofPattern("dd");
					String dataVencimento = request.getDataPrimeiroVencimento().format(formatterDia) + "/" + p.getMesAnoVencimento();
					i.setVencimento(LocalDate.parse(dataVencimento, formatter));
				}
				intermediarias.add(i);
			}
			setIntermediarias(intermediarias);
		}
		setVlrSeguroFranquia(request.getValorSeguroFranquia());
		setModalidade(Modalidade.CDC);
		setIsSimulacaoInterna(true);
		setCarenciaDoPlano(new Carencia(Constants.ZERO_INTEIRO, Constants.ZERO_INTEIRO));
		setIsentaIOF("F");
		setPermiteBalao(true);

	}
}
