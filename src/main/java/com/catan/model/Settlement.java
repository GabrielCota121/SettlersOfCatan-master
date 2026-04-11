package com.catan.model;

public class Settlement extends VertexBuilding {

    public Settlement(Player owner, Vertex location) {
        super(owner, location);
    }

    @Override
    public int getVictoryPoints() {
        return 1;
    }

    @Override
    public int getResourceYield() {
        return 1;
    }
}