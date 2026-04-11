package com.catan.model;

public class ConsoleLogger implements GameLogger {
    @Override
    public void log(String message) {
        System.out.println("[INFO] " + message);
    }

    @Override
    public void error(String message) {
        System.err.println("[ERRO] " + message);
    }
}