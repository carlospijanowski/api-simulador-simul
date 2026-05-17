package br.com.bancotoyota.services.simulador.services.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Erro implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 7491084350166961638L;
    @Schema(name = "field", description = "Atributo que está com erro.", example = "plano-id")
    private String field;
    @Schema(name = "value", description = "Valor do atributo que está com erro.", example = "7074")
    private String value;
    @Schema(name = "id", description = "Codigo do atributo que está com erro.", example = "712")
    private String id;
    @Schema(name = "message", description = "Descrição de erro.", example = "plano selecionado não suporta subsídio variável")
    private String message;

    public Erro(String message) {
        this.message = message;
    }
    
    public Erro(String field, String value , String message) {
    	this.field = field;
    	this.value = value;
    	this.message = message;
    }
}
