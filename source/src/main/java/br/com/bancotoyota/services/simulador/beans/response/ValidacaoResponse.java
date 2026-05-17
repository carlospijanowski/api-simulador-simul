package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.beans.ParametrosDeSeguroPrestamista;
import br.com.bancotoyota.services.simulador.services.exceptions.Erro;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ValidacaoResponse {
    private List<Erro> erros;
    private LocalDate dataNascimento;

    private ParametrosDeSeguroPrestamista parametros;
}
