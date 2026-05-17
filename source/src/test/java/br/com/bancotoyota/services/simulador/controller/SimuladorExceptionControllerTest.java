package br.com.bancotoyota.services.simulador.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimuladorExceptionControllerTest {

    @Getter
    @Setter
    public static class Bean1 {

        @JsonProperty("bean-2")
        private Bean2 bean2;
    }

    @Getter
    @Setter
    public static class SubBean1 extends Bean1 {
    }

    @Getter
    @Setter
    public static class Bean2 {

        @JsonProperty("sobre-nome")
        private String nome;

        private String endereco;
    }

    private SimuladorExceptionController controller = new SimuladorExceptionController(
            null, null, null, null, false, null, null, null) {};

    @Test
    public void testaPathTraduzido() {
        String str = controller.pathTraduzido(Bean1.class,"bean2.nome");
        assertEquals("bean-2.sobre-nome", str);
    }

    @Test
    public void testaPathTraduzidoSubClasse() {
        String str = controller.pathTraduzido(SubBean1.class,"bean2.nome");
        assertEquals("bean-2.sobre-nome", str);
    }

    @Test
    public void testaPathTraduzidoCampoInexistente() {
        String str = controller.pathTraduzido(SubBean1.class,"bean2.idade");
        assertEquals("bean-2.idade", str);
    }

    @Test
    public void testaPathTraduzidoSemAnotacao() {
        String str = controller.pathTraduzido(SubBean1.class,"bean2.endereco");
        assertEquals("bean-2.endereco", str);
    }
}