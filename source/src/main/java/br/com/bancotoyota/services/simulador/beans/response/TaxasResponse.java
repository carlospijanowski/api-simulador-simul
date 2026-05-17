package br.com.bancotoyota.services.simulador.beans.response;

import br.com.bancotoyota.services.simulador.entities.Taxa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class TaxasResponse {

    Taxa taxa;
}
