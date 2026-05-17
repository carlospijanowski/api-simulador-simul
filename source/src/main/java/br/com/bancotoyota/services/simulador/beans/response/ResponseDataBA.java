package br.com.bancotoyota.services.simulador.beans.response;

import lombok.Getter;

@Getter
public class ResponseDataBA {

    private DataBAResponse response;

    public ResponseDataBA(DataBAResponse response){
        this.response = response;
    }
}
