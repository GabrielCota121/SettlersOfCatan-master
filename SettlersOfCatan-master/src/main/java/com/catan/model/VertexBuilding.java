package com.catan.model;

public abstract class VertexBuilding extends Building {
    public VertexBuilding(Player owner) {
        super(owner);
    }
    public abstract int getVictoryPoints();
    public abstract int getResourceYield();
}