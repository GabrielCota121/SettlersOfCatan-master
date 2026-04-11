package com.catan.model;

public class City extends VertexBuilding {

    public City(Player owner, Vertex location) {
        super(owner, location);
    }

    @Override
    public int getVictoryPoints() {
        return 2;
    }

    @Override
    public int getResourceYield() {
        return 2;
    }
}