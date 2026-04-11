package com.catan.model;

public class Player {
    private int id;
    private String name;
    private String color;
    private final ResourceWallet wallet;

    public Player(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.wallet = new ResourceWallet();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public ResourceWallet getWallet() { return wallet; }
}
