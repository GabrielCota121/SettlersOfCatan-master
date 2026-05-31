package com.catan.model.logging;

public class WebSocketLogger implements IGameLogger {
    @Override
    public void log(String message) {
    }

    @Override
    public void error(String message) {
        System.err.println("[ERRO] " + message);
    }
}