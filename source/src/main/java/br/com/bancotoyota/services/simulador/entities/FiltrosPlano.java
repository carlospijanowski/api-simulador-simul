package br.com.bancotoyota.services.simulador.entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@AllArgsConstructor
public class FiltrosPlano {
    private ConfiguracaoPlano configuracao;
    private String aplicacao;
    private String cnpjOrigemNegocio;
    private String regiao;
    private String marca;
    private String modelo;
    private Integer anoModelo;
    private String perfilUsuario;

    /**
     * O time do OBO criou o código para encontrar um plano (chamado resta um), no entanto eles fazem algumas filtragens
     * que não tem sentido para o Direct, como verificar o tipo de subsídio após terem carregado somente os planos sem subsídio.
     * Esse atributo controla se vamos executar esse workaround ou não.
     */
    private boolean workaroundObo;
    private List<String> idsModalidade;
}
