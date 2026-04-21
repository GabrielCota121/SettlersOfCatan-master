package com.catan.model.building;

import com.catan.model.player.Player;
import com.catan.model.board.Vertex;

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