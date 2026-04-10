package com.catan.model;

public class Settlement extends VertexBuilding {
    public Settlement(Player owner) { super(owner); }

    @Override
    public int getVictoryPoints() { return 1; }

    @Override
    public int getResourceYield() { return 1; }
}