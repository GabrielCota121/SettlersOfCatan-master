package com.example.model.board;

import com.example.model.game.ResourceType;

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