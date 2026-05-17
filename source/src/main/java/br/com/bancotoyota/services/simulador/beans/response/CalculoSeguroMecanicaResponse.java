package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.entities.SeguroMecanica;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class CalculoSeguroMecanicaResponse {
    @Schema(name = "cotacoes", description = "Cotações disponiveis.")
    private List<SeguroMecanica> cotacoes;
    @Schema(name = "selecionado", description = "Seguro selecionado.")
    private SeguroMecanica selecionado;
    @Schema(name = "erros", description = "Mensagens de erro de elegibilidade.")
    private List<Erro> erros = new ArrayList<>();
}
