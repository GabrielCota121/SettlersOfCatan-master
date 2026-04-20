package com.catan.model;

import java.util.HashMap;
import java.util.Map;

public class Player {
    private int id;
    private String name;
    private String color;
    private final ResourceWallet wallet;
    private final Map<ResourceType, Integer> tradeRates;

    public Player(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.wallet = new ResourceWallet();
        this.tradeRates = new HashMap<>();
        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.DESERT) {
                tradeRates.put(type, 4);
            }
        }
    }

    public void setTradeRate(ResourceType type, int newRate) {
        if (tradeRates.getOrDefault(type, 4) > newRate) {
            tradeRates.put(type, newRate);
        }
    }

    public int getTradeRate(ResourceType type) {
        return tradeRates.getOrDefault(type, 4);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public ResourceWallet getWallet() { return wallet; }
}
