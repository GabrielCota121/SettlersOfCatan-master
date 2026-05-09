package com.catan.network.Exception;

public class PortaEmUsoException extends Exception {
    public PortaEmUsoException(int porta) {
        super("Porta "+porta+" em uso. Escolha outra porta.");
    }
}
