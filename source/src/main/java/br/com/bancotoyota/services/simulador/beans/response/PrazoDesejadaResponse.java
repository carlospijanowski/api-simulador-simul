package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.ResultadoCalculadoParcelaDesejada;
import br.com.bancotoyota.services.simulador.entities.Subsidio;
import lombok.*;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PrazoDesejadaResponse extends PrazoResidualResponse {

    public PrazoDesejadaResponse(ResultadoCalculadoParcelaDesejada resultado, Subsidio dadosSubsidio) {
        super(resultado, dadosSubsidio);
    }
}
