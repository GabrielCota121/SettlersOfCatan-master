package com.catan.network.exception;

public class PortaInvalidaException extends Exception {
    public PortaInvalidaException(int porta) {
        super("Porta "+porta+" inválida. Escolha um número entre 1 e 25565.");
    }
}
