package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.entities.CestaItem;
import br.com.bancotoyota.services.simulador.entities.CestaServico;
import br.com.bancotoyota.services.simulador.entities.Plano;
import br.com.bancotoyota.services.simulador.entities.Retorno;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/**
 * Para a primeira versão vamos retornar os objetos completos para que não tenham que ser novamente carregados no
 * serviço de propostas.
 */
public class DadosAlternativa {

    private CestaServico cesta;

    private List<CestaItem> cestaItens;

    private Plano plano;

    private Retorno retorno;
}
