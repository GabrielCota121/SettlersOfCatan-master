package com.catan.model;

public class Port {
    private final ResourceType resource;
    private final int exchangeRate;

    public Port(ResourceType resource, int exchangeRate) {
        this.resource = resource;
        this.exchangeRate = exchangeRate;
    }

    public ResourceType getResource() { return resource; }
    public int getExchangeRate() { return exchangeRate; }
}