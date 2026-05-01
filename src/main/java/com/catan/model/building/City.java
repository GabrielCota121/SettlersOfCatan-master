package com.catan.model.building;

import com.catan.model.player.Player;
import com.catan.model.board.Vertex;

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