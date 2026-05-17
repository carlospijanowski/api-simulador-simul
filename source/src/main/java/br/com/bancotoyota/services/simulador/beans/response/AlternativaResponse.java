package br.com.bancotoyota.services.simulador.beans.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlternativaResponse {

    private SimulacaoParcDesejadaResponse simulacoes;

    /**
     * Indica os valores escolhidos para a alternativa, como novo plano, nova cesta, novo retorno e estado.
     */
    private DadosAlternativa dadosAlternativa;
}
