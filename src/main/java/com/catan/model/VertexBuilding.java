package com.catan.model;

public abstract class VertexBuilding {
    protected Player owner;
    protected Vertex location;

    public VertexBuilding(Player owner, Vertex location) {
        this.owner = owner;
        this.location = location;
    }

    public Player getOwner() {
        return owner;
    }

    public Vertex getLocation() {
        return location;
    }

    public abstract int getVictoryPoints();

    public abstract int getResourceYield();
}