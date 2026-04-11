package com.catan.model;

public abstract class EdgeBuilding {
    protected Player owner;
    protected Edge location;

    public EdgeBuilding(Player owner, Edge location) {
        this.owner = owner;
        this.location = location;
    }

    public Player getOwner() {
        return owner;
    }

    public Edge getLocation() {
        return location;
    }
}