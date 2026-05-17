package br.com.bancotoyota.services.simulador.beans.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaResidual;
import br.com.bancotoyota.services.simulador.beans.request.SimulacaoTaxaRequest;
import br.com.bancotoyota.services.simulador.entities.Subsidio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Leandro Haruo Aoyagi
 * Bean com os dados da simulação
 */

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class SimulacaoTaxaResponse {

    @Schema(name = "valor-parcela", description = "Valor da parcela do financiamento.", example = "123.56")
	@JsonProperty("valor-parcela")
	private BigDecimal valorParcela;
	
	@JsonProperty("subsidio")
	private ValorSubsidio subsidio;

    @Schema(name = "valor-iof", description = "Valor total do IOF do financiamento.", example = "2137.85")
    @JsonProperty("valor-iof")
	private BigDecimal valorIOF;

    @Schema(name = "valor-iof-adicional", description = "Valor do IOF adicional do financiamento.", example = "0.09")
	@JsonProperty("valor-iof-adicional")
	private BigDecimal valorIofAdicional;

    @Schema(name = "valor-cet-anual", description = "Valor do custo efetivo anual do financiamento.", example = "23.02")
	@JsonProperty("valor-cet-anual")
	private BigDecimal valorCetAnual;
	
	@JsonProperty("fluxo")
    private List<Fluxo> fluxo;
	
	public SimulacaoTaxaResponse (ResultadoCalculadoParcelaResidual resultado, Subsidio dadosSubsidio, BigDecimal valorIofAdicional) {

        setValorParcela(resultado.getValorParcela().setScale(2, RoundingMode.DOWN));
        
        BigDecimal valorSubsidio = duasCasasDecimais(resultado.getValorSubsidio());
        if (dadosSubsidio != null) {
            setSubsidio(dadosSubsidio.fazerDistribuicao(valorSubsidio));
        }
        
        setValorIOF(resultado.getValorIOF().subtract(valorIofAdicional));
        setValorIofAdicional(valorIofAdicional);
        
        setValorCetAnual(resultado.getValorCetAnual());
        
		fluxo = resultado.getFluxo().stream().filter(f -> f.getParcela() > 0).collect(Collectors.toList());
		
		
	}
	
    private BigDecimal duasCasasDecimais(BigDecimal valor) {
        if (valor != null) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        } else {
            return null;
        }
    }
    
}
