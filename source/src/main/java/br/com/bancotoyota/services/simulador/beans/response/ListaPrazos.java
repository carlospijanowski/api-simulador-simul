package br.com.bancotoyota.services.simulador.beans.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ListaPrazos {

    List<Integer> prazos;
}
