package br.com.bancotoyota.services.simulador.beans;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Esta classe é usada quando depois de ter feito a primeira simulação por parcela desejada e depois de ter selecionado
 * um prazo, o usuário remove algum elemento, como por exemplo um seguro.
 * Nesse caso ao fazer o recálculo o valor do residual para a prazo selecionado será mantido fixo. Para os outros prazos
 * o cálculo correrá normalmente.
 */
@Getter
@Setter
@NoArgsConstructor
public class PrazoSelecionado {

    @NotNull
    private BigDecimal residual;

    @NotNull
    private int prazo;
}
