package com.catan.network.exception;

public class ConexaoNaoEstabelecidaException extends Exception {
    public ConexaoNaoEstabelecidaException() {
        super("Não foi estabelecida uma conexão com uma partida.");
    }
}
