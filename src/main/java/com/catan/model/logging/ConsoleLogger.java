package com.catan.model.logging;

public class ConsoleLogger implements IGameLogger {
    @Override
    public void log(String message) {
        System.out.println(message);
    }

    @Override
    public void error(String message) {
        System.err.println("[ERRO] " + message);
    }
}