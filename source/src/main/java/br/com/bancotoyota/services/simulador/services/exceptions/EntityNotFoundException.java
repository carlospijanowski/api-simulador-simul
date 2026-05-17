package br.com.bancotoyota.services.simulador.services.exceptions;


public class EntityNotFoundException extends RuntimeException {

    private final Erro erro;
    private static final String MESSAGE = "%s não encontrado(a)!";

    public EntityNotFoundException(Class clazz, String id) {
        super(String.format(MESSAGE, clazz.getSimpleName().replace("DTO", "") + ": " + id));
        this.erro = new Erro(getMessage());
    }

    public EntityNotFoundException(Erro erro) {
        super(erro.getMessage());
        this.erro = erro;
    }

    public Erro getErro() {
        return erro;
    }
}
