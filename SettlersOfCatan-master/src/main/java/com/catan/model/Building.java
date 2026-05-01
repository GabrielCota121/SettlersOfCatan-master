package com.catan.model;

public abstract class Building {
    protected Player owner;

    public Building(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }
}