package com.catan.network.exception;

public class PortaEmUsoException extends Exception {
    public PortaEmUsoException(int porta) {
        super("Porta "+porta+" em uso. Escolha outra porta.");
    }
}
